package ge.opensoft.openhex;

import static ge.opensoft.openhex.Core.*;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.regex.*;

@RestController
public class ApiController {
    private final Core core;
    private static final Map<String, String> TYPES =
            Map.of("CODE", "codeCell", "SQL", "sqlCell", "MARKDOWN", "markdownCell");

    public ApiController(Core core) {
        this.core = core;
    }

    @RequestMapping("/**")
    public ResponseEntity<?> dispatch(
            HttpServletRequest request, @RequestBody(required = false) byte[] bytes)
            throws Exception {
        return core.execute(
                request,
                bytes,
                this::route,
                e ->
                        map(
                                "details",
                                e.getMessage(),
                                "traceId",
                                UUID.randomUUID().toString(),
                                "reason",
                                e.title()));
    }

    private Result route() throws Exception {
        String path = core.request().getRequestURI(), method = core.request().getMethod();
        if (path.equals("/api/v1/projects")) {
            requireMethod(method, "GET", "POST");
            return projects(method);
        }
        if (path.equals("/api/v1/cells")) {
            requireMethod(method, "GET", "POST");
            return cells(method);
        }
        Matcher route = Pattern.compile("/api/v1/(projects|cells)/([^/]+)").matcher(path);
        if (!route.matches()) throw new Failure(404, "Resource not found");
        core.parameters();
        String id = route.group(2);
        if (route.group(1).equals("projects")) {
            requireMethod(method, "GET", "PATCH");
            var item = core.get("project", id);
            if (method.equals("GET")) return ok(item);
            if (!core.actor().get("role").equals("admin"))
                throw new Failure(403, "Only administrators may set project status");
            var body = object(core.body(), "status");
            if (body.get("status") != null) text(body.get("status"), 100, false);
            item.put("status", body.get("status") == null ? null : map("name", body.get("status")));
            item.put("lastEditedAt", timestamp());
            return ok(core.save("project", id, item));
        }
        requireMethod(method, "GET", "PATCH", "DELETE");
        var item = core.get("cell", id);
        if (method.equals("GET")) return ok(item);
        core.writable();
        String projectId = (String) item.get("projectId");
        if (method.equals("DELETE")) {
            var order = core.get("order", projectId);
            ids(order).remove(id);
            core.save("order", projectId, order);
            core.delete("cell", id);
            touch(projectId);
            return ok(map("cellId", id));
        }
        var body = object(core.body(), "contents");
        var inner = contents(body.get("contents"), (String) item.get("cellType"));
        var variants = cast(item.get("contents"));
        cast(variants.get(TYPES.get(item.get("cellType")))).putAll(inner);
        touch(projectId);
        return ok(core.save("cell", id, item));
    }

    private Result projects(String method) throws Exception {
        if (method.equals("GET")) {
            core.parameters("limit", "after", "before", "creatorEmail", "ownerEmail");
            var data = new ArrayList<>(core.rows("project").reversed());
            for (String field : List.of("creator", "owner")) {
                String filter = core.argument(field + "Email", null);
                if (filter != null) {
                    text(filter, 255, false);
                    data.removeIf(project ->
                            !Objects.equals(cast(project.get(field)).get("email"), filter));
                }
            }
            return ok(paginate(data));
        }
        core.parameters();
        core.writable();
        var body = object(core.body(), Set.of("title", "description"), Set.of("title"));
        text(body.get("title"), 255, false);
        if (body.containsKey("description")) text(body.get("description"), 65536, true);
        String id = UUID.randomUUID().toString(), now = timestamp();
        var project =
                map(
                        "id",
                        id,
                        "title",
                        body.get("title"),
                        "description",
                        body.get("description"),
                        "type",
                        "PROJECT",
                        "computeProfile",
                        map("LOGIC", map("kernelImage", "python312", "kernelSize", "small")),
                        "creator",
                        map("email", core.actor().get("user")),
                        "owner",
                        map("email", core.actor().get("user")),
                        "status",
                        null,
                        "categories",
                        List.of(),
                        "reviews",
                        map("required", false),
                        "analytics",
                        map(
                                "appViews",
                                map(
                                        "allTime",
                                        0,
                                        "lastSevenDays",
                                        0,
                                        "lastFourteenDays",
                                        0,
                                        "lastThirtyDays",
                                        0),
                                "lastViewedAt",
                                null,
                                "publishedResultsUpdatedAt",
                                null),
                        "lastEditedAt",
                        now,
                        "lastPublishedAt",
                        null,
                        "createdAt",
                        now,
                        "archivedAt",
                        null,
                        "trashedAt",
                        null,
                        "schedules",
                        List.of(),
                        "folderEntryId",
                        null);
        core.insert("order", id, map("ids", List.of()));
        return created(core.insert("project", id, project));
    }

    private Result cells(String method) throws Exception {
        if (method.equals("GET")) {
            core.parameters("projectId", "limit", "after", "before");
            String projectId = core.argument("projectId", "");
            core.get("project", projectId);
            var data = new ArrayList<Map<String, Object>>();
            for (String id : ids(core.get("order", projectId))) data.add(core.get("cell", id));
            return ok(paginate(data));
        }
        core.parameters();
        core.writable();
        var body =
                object(
                        core.body(),
                        Set.of("projectId", "cellType", "contents", "label", "location"),
                        Set.of("projectId", "cellType", "contents"));
        String projectId = text(body.get("projectId"));
        core.get("project", projectId);
        if (!(body.get("cellType") instanceof String type) || !TYPES.containsKey(type))
            throw new Failure(400, "Supported types: CODE, SQL, MARKDOWN");
        var inner = contents(body.get("contents"), type);
        if (body.containsKey("label")) text(body.get("label"), 255, true);
        String id = UUID.randomUUID().toString();
        if (type.equals("SQL"))
            inner.putIfAbsent("outputDataframe", "dataframe_" + id.replace('-', '_'));
        var variants = map("codeCell", null, "sqlCell", null, "markdownCell", null);
        variants.put(TYPES.get(type), inner);
        var item =
                map(
                        "id",
                        id,
                        "staticId",
                        UUID.randomUUID().toString(),
                        "cellType",
                        type,
                        "projectId",
                        projectId,
                        "label",
                        body.get("label"),
                        "dataConnectionId",
                        null,
                        "contents",
                        variants);
        var order = core.get("order", projectId);
        var ids = ids(order);
        int position = ids.size();
        if (body.containsKey("location")) {
            var location = object(body.get("location"), "insertAfterCellId");
            if (!ids.contains(location.get("insertAfterCellId")))
                throw new Failure(400, "Insertion anchor must belong to this project");
            position = ids.indexOf(location.get("insertAfterCellId")) + 1;
        }
        ids.add(position, id);
        core.save("order", projectId, order);
        touch(projectId);
        return ok(core.insert("cell", id, item));
    }

    private Map<String, Object> contents(Object value, String type) {
        String variant = TYPES.get(type);
        var body = object(value, variant);
        var inner =
                object(
                        body.get(variant),
                        type.equals("SQL") ? Set.of("source", "outputDataframe") : Set.of("source"),
                        Set.of("source"));
        text(inner.get("source"), 65536, true);
        if (inner.containsKey("outputDataframe")) {
            String identifier = text(inner.get("outputDataframe"), 100, false);
            if (!identifier.matches(
                    "[_\\p{L}\\p{Nl}][_\\p{L}\\p{Nl}\\p{Nd}\\p{Mn}\\p{Mc}\\p{Pc}]*"))
                throw new Failure(400, "outputDataframe must be a valid identifier");
        }
        return inner;
    }

    private Map<String, Object> paginate(List<Map<String, Object>> data) {
        int limit = integer(core.argument("limit", "25"), 1, 100), start = 0, end = data.size();
        String after = core.argument("after", null), before = core.argument("before", null);
        if (after != null && before != null)
            throw new Failure(400, "Use after or before, not both");
        var ids = data.stream().map(item -> item.get("id")).toList();
        if (after != null || before != null) {
            int index = ids.indexOf(after != null ? after : before);
            if (index < 0) throw new Failure(400, "Cursor is not in this result set");
            if (after != null) start = index + 1;
            else {
                end = index;
                start = Math.max(0, end - limit);
            }
        }
        var page = data.subList(start, Math.min(end, start + limit));
        return map(
                "values",
                page,
                "pagination",
                map(
                        "after",
                        !page.isEmpty() && start + page.size() < data.size()
                                ? page.getLast().get("id")
                                : null,
                        "before",
                        !page.isEmpty() && start > 0 ? page.getFirst().get("id") : null));
    }

    private void touch(String id) throws Exception {
        var item = core.get("project", id);
        item.put("lastEditedAt", timestamp());
        core.save("project", id, item);
    }

    @SuppressWarnings("unchecked")
    private static List<String> ids(Map<String, Object> order) {
        return (List<String>) order.get("ids");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(Object value) {
        return (Map<String, Object>) value;
    }

    private static void requireMethod(String actual, String... methods) {
        if (!List.of(methods).contains(actual)) throw new Failure(405, "Method not allowed");
    }
}
