const API = "/api";
const SESSION_KEY = "archmaxim.session";
const SEARCH_LIMIT = 20;
const ADMIN_PAGE_SIZE = 200;
const INDEX_POLL_MS = 2000;
const MAX_TIMER_MS = 2147483647;

const FILE_TYPES = [
    { value: "MIDTERM", label: "Midterm" },
    { value: "FINAL", label: "Final" },
    { value: "QUIZ", label: "Quiz" },
    { value: "HOMEWORK", label: "Homework" },
    { value: "NOTE", label: "Lecture notes" },
    { value: "SYLLABUS", label: "Syllabus" },
    { value: "LAB", label: "Lab" },
    { value: "PROJECT", label: "Project" },
    { value: "OTHER", label: "Other" },
];

const SEASONS = [
    { value: "FALL", label: "Fall" },
    { value: "SPRING", label: "Spring" },
    { value: "SUMMER", label: "Summer" },
];

const TITLES = [
    { value: "PROFESSOR", label: "Professor", short: "Prof." },
    { value: "ASSOCIATE_PROFESSOR", label: "Associate Professor", short: "Assoc. Prof." },
    { value: "ASSISTANT_PROFESSOR", label: "Assistant Professor", short: "Asst. Prof." },
    { value: "LECTURER", label: "Lecturer", short: "Lect." },
    { value: "RESEARCH_ASSISTANT", label: "Research Assistant", short: "Res. Asst." },
    { value: "INSTRUCTOR", label: "Instructor", short: "Instr." },
];

const ROLE_LABELS = { ADMIN: "Admin", LECTURER: "Lecturer", STUDENT: "Student" };

const STATUS_MESSAGES = {
    400: "Some of the information is not valid.",
    401: "Please log in again.",
    403: "You are not allowed to do that.",
    404: "That item no longer exists.",
    409: "That already exists.",
    413: "The file is too large.",
    429: "Too many attempts. Try again later.",
    503: "The search service is starting up. Try again in a minute.",
};

const $ = (id) => document.getElementById(id);

const currentYear = new Date().getFullYear();

const searchForm = $("searchForm");
const queryInput = $("query");
const searchBtn = $("searchBtn");
const typeFilter = $("typeFilter");
const seasonFilter = $("seasonFilter");
const yearFilter = $("yearFilter");
const departmentFilter = $("departmentFilter");
const mineToggle = $("mineToggle");
const mineFilter = $("mineFilter");
const loginNote = $("loginNote");
const resultsMeta = $("resultsMeta");
const resultsDiv = $("results");

const openLoginBtn = $("openLoginBtn");
const openUploadBtn = $("openUploadBtn");
const openAdminBtn = $("openAdminBtn");
const openPasswordBtn = $("openPasswordBtn");
const logoutBtn = $("logoutBtn");

const loginForm = $("loginForm");
const uploadForm = $("uploadForm");
const uploadHint = $("uploadHint");
const passwordForm = $("passwordForm");
const toasts = $("toasts");

let session = readSession();
let expiryTimer = null;
let pendingSearch = false;
let lastQuery = "";
let resultCount = 0;

function el(tag, attrs = {}, ...children) {
    const node = document.createElement(tag);
    for (const [key, value] of Object.entries(attrs)) {
        if (value == null || value === false) continue;
        if (key === "class") node.className = value;
        else node.setAttribute(key, value === true ? "" : value);
    }
    node.append(...children.flat().filter((child) => child != null && child !== false));
    return node;
}

function labelOf(list, value, key = "label") {
    return list.find((item) => item.value === value)?.[key] ?? value ?? "";
}

function enumOptions(placeholder) {
    return { placeholder, value: (item) => item.value, label: (item) => item.label };
}

function setOptions(select, items, { placeholder, value = (item) => item.id, label }) {
    const previous = select.value;
    const options = items.map((item) => el("option", { value: value(item) }, label(item)));
    if (placeholder !== undefined) options.unshift(el("option", { value: "" }, placeholder));
    select.replaceChildren(...options);
    if ([...select.options].some((option) => option.value === previous)) select.value = previous;
}

function submitOf(form) {
    return form.querySelector('[type="submit"]');
}

async function withBusy(button, task, busyLabel) {
    if (!button || button.disabled) return;
    const label = button.textContent;
    button.disabled = true;
    button.setAttribute("aria-busy", "true");
    if (busyLabel) button.textContent = busyLabel;
    try {
        return await task();
    } finally {
        button.disabled = false;
        button.removeAttribute("aria-busy");
        if (busyLabel) button.textContent = label;
    }
}

// The browser accepts "   " for a required field, the API does not.
function firstBlankField(form) {
    for (const field of form.querySelectorAll("input[required], textarea[required]")) {
        if (field.type !== "file" && field.type !== "password" && !field.value.trim()) return field;
    }
    return null;
}

function rejectBlank(form) {
    const field = firstBlankField(form);
    if (!field) return false;
    const name = field.closest("label")?.querySelector("span")?.firstChild?.textContent.trim() ?? "This field";
    toast(`${name} cannot be empty`, "error");
    field.focus();
    return true;
}

function personName(person) {
    return `${person.firstName} ${person.lastName}`;
}

function courseLabel(course) {
    return `${course.code} ${course.name}`;
}

function formatSize(bytes) {
    if (bytes == null) return null;
    const units = ["B", "KB", "MB", "GB"];
    let size = bytes;
    let unit = 0;
    while (size >= 1024 && unit < units.length - 1) {
        size /= 1024;
        unit++;
    }
    return `${size.toFixed(unit === 0 || size >= 10 ? 0 : 1)} ${units[unit]}`;
}

function plural(count, word) {
    return `${count} ${word}${count === 1 ? "" : "s"}`;
}

function formatTime(iso) {
    return iso ? new Date(iso).toLocaleString([], { dateStyle: "medium", timeStyle: "short" }) : "";
}

function humanize(field) {
    const words = field.replace(/([A-Z])/g, " $1").toLowerCase();
    return words.charAt(0).toUpperCase() + words.slice(1);
}

function readSession() {
    try {
        // Keys written by the first version of the app, before sessions expired.
        ["token", "name", "role"].forEach((key) => localStorage.removeItem(key));
        const stored = JSON.parse(localStorage.getItem(SESSION_KEY));
        if (stored?.token && Date.parse(stored.expiresAt) > Date.now()) return stored;
        localStorage.removeItem(SESSION_KEY);
    } catch {
        // Storage can be blocked or hold something unreadable; start signed out.
    }
    return null;
}

function tokenExpiry(token) {
    try {
        const payload = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
        const { exp } = JSON.parse(atob(payload));
        return exp ? new Date(exp * 1000).toISOString() : null;
    } catch {
        return null;
    }
}

function saveSession(data) {
    session = {
        token: data.token,
        id: data.id,
        name: data.name,
        role: data.role,
        expiresAt: data.expiresAt ?? tokenExpiry(data.token),
    };
    try {
        localStorage.setItem(SESSION_KEY, JSON.stringify(session));
    } catch {
        // Without storage the session simply ends with the tab.
    }
    scheduleExpiry();
}

function clearSession() {
    session = null;
    clearTimeout(expiryTimer);
    try {
        localStorage.removeItem(SESSION_KEY);
    } catch {
        // Nothing to clean up.
    }
}

function scheduleExpiry() {
    clearTimeout(expiryTimer);
    if (!session) return;
    const remaining = Date.parse(session.expiresAt) - Date.now();
    if (!(remaining > 0)) {
        expireSession();
        return;
    }
    expiryTimer = setTimeout(scheduleExpiry, Math.min(remaining, MAX_TIMER_MS));
}

function expireSession() {
    if (!session) return;
    clearSession();
    closeAllModals();
    updateAuthUI();
    openModal("loginModal");
    toast("Your session has expired. Please log in again.", "error");
}

class ApiError extends Error {
    constructor(message, status, { expired = false } = {}) {
        super(message);
        this.status = status;
        this.expired = expired;
    }
}

async function readProblem(response) {
    try {
        if ((response.headers.get("Content-Type") || "").includes("json")) return await response.json();
    } catch {
        // Fall back to the generic message for this status.
    }
    return {};
}

function problemMessage(problem, response) {
    const fieldErrors = problem.errors ? Object.entries(problem.errors) : [];
    if (fieldErrors.length) {
        const [field, message] = fieldErrors[0];
        return /^[A-Z]/.test(message) ? message : `${humanize(field)} ${message}`;
    }
    if (problem.detail) return problem.detail;
    const retryAfter = Number(response.headers.get("Retry-After"));
    if (response.status === 429 && retryAfter > 0) {
        return `Too many attempts. Try again in ${plural(Math.ceil(retryAfter / 60), "minute")}.`;
    }
    return STATUS_MESSAGES[response.status] ?? `Something went wrong (${response.status}).`;
}

async function api(path, { method = "GET", body, form, raw = false } = {}) {
    const headers = {};
    const authenticated = Boolean(session);
    if (authenticated) headers.Authorization = `Bearer ${session.token}`;

    let payload = form;
    if (body !== undefined) {
        headers["Content-Type"] = "application/json";
        payload = JSON.stringify(body);
    }

    let response;
    try {
        response = await fetch(API + path, { method, headers, body: payload });
    } catch {
        throw new ApiError("Could not reach the server.", 0);
    }

    if (!response.ok) {
        const problem = await readProblem(response);
        if (response.status === 401 && authenticated) {
            expireSession();
            throw new ApiError("Your session has expired. Please log in again.", 401, { expired: true });
        }
        throw new ApiError(problemMessage(problem, response), response.status);
    }

    if (raw) return response;
    if (response.status === 204) return null;
    return (response.headers.get("Content-Type") || "").includes("json") ? response.json() : null;
}

// An expired session has already been announced by expireSession().
function report(error) {
    if (!error.expired) toast(error.message, "error");
}

function toast(message, kind = "info") {
    const node = el("div", { class: `toast ${kind}` }, message);
    let gone = false;
    const dismiss = () => {
        if (gone) return;
        gone = true;
        node.classList.add("fade-out");
        setTimeout(() => node.remove(), 300);
    };
    node.addEventListener("click", dismiss);
    toasts.append(node);
    setTimeout(dismiss, kind === "error" ? 5000 : 3200);
}

const openModals = [];
const FOCUSABLE = "a[href], button, input, select, textarea, [tabindex]";

function focusableIn(container) {
    return [...container.querySelectorAll(FOCUSABLE)]
        .filter((node) => !node.disabled && node.tabIndex >= 0 && node.type !== "hidden" && node.offsetParent !== null);
}

function openModal(id) {
    const backdrop = $(id);
    if (openModals.some((entry) => entry.backdrop === backdrop)) return;
    openModals.push({ backdrop, opener: document.activeElement });
    backdrop.classList.add("open");
    document.body.classList.add("modal-open");
    const first = focusableIn(backdrop).find((node) => !node.matches(".modal-close"));
    (first ?? backdrop.querySelector(".modal")).focus();
}

function closeModal(target) {
    const backdrop = typeof target === "string" ? $(target) : target;
    const index = openModals.findIndex((entry) => entry.backdrop === backdrop);
    if (index === -1) return;
    const [{ opener }] = openModals.splice(index, 1);
    backdrop.classList.remove("open");
    backdrop.dispatchEvent(new Event("modalclose"));
    if (!openModals.length) document.body.classList.remove("modal-open");
    if (opener?.isConnected && opener.offsetParent !== null) opener.focus();
}

function closeAllModals() {
    [...openModals].reverse().forEach(({ backdrop }) => closeModal(backdrop));
}

document.querySelectorAll("[data-close]").forEach((button) => {
    button.addEventListener("click", () => closeModal(button.closest(".modal-backdrop")));
});

document.querySelectorAll(".modal-backdrop").forEach((backdrop) => {
    let pressedOnBackdrop = false;
    backdrop.addEventListener("pointerdown", (event) => {
        pressedOnBackdrop = event.target === backdrop;
    });
    backdrop.addEventListener("click", (event) => {
        if (pressedOnBackdrop && event.target === backdrop) closeModal(backdrop);
    });
});

document.addEventListener("keydown", (event) => {
    const top = openModals.at(-1);
    if (!top) return;
    if (event.key === "Escape") {
        event.preventDefault();
        closeModal(top.backdrop);
        return;
    }
    if (event.key !== "Tab") return;

    const nodes = focusableIn(top.backdrop);
    if (!nodes.length) return;
    const first = nodes[0];
    const last = nodes.at(-1);
    const outside = !top.backdrop.contains(document.activeElement);
    if (event.shiftKey && (outside || document.activeElement === first)) {
        event.preventDefault();
        last.focus();
    } else if (!event.shiftKey && (outside || document.activeElement === last)) {
        event.preventDefault();
        first.focus();
    }
});

function updateAuthUI() {
    const role = session?.role;
    const signedIn = Boolean(session);

    $("loggedOut").hidden = signedIn;
    $("loggedIn").hidden = !signedIn;
    loginNote.hidden = signedIn;
    openUploadBtn.hidden = role !== "LECTURER";
    openAdminBtn.hidden = role !== "ADMIN";
    departmentFilter.hidden = !signedIn;
    mineToggle.hidden = role !== "LECTURER" && role !== "STUDENT";
    if (mineToggle.hidden) mineFilter.checked = false;

    if (signedIn) {
        $("userName").textContent = session.name;
        $("userRole").textContent = ROLE_LABELS[role] ?? role;
        $("userInitial").textContent = session.name.charAt(0).toUpperCase();
    } else {
        lastQuery = "";
        resultCount = 0;
        resultsMeta.textContent = "";
        resultsDiv.replaceChildren();
    }
}

async function loadDepartmentFilter() {
    try {
        const departments = await api("/departments");
        setOptions(departmentFilter, departments, { placeholder: "All departments", label: (d) => d.name });
    } catch (error) {
        report(error);
    }
}

function logout() {
    clearSession();
    closeAllModals();
    updateAuthUI();
    toast("Logged out");
}

loginForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (rejectBlank(loginForm)) return;
    const { email, password } = loginForm.elements;

    await withBusy(submitOf(loginForm), async () => {
        try {
            const data = await api("/auth/login", {
                method: "POST",
                body: { email: email.value.trim(), password: password.value },
            });
            const searchAfterLogin = pendingSearch;
            saveSession(data);
            password.value = "";
            closeModal("loginModal");
            updateAuthUI();
            loadDepartmentFilter();
            toast(`Welcome, ${session.name}`, "success");
            if (searchAfterLogin) search();
        } catch (error) {
            toast(error.message, "error");
            password.select();
        }
    }, "Logging in…");
});

passwordForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const { currentPassword, newPassword, confirmPassword } = passwordForm.elements;
    if (newPassword.value !== confirmPassword.value) {
        toast("The new passwords do not match", "error");
        confirmPassword.focus();
        return;
    }
    if (newPassword.value === currentPassword.value) {
        toast("Pick a password different from the current one", "error");
        newPassword.focus();
        return;
    }

    await withBusy(submitOf(passwordForm), async () => {
        try {
            await api("/auth/change-password", {
                method: "POST",
                body: { currentPassword: currentPassword.value, newPassword: newPassword.value },
            });
            passwordForm.reset();
            closeModal("passwordModal");
            toast("Password changed", "success");
        } catch (error) {
            report(error);
        }
    });
});

$("passwordModal").addEventListener("modalclose", () => passwordForm.reset());

function searchParams(query) {
    const params = new URLSearchParams({ query, limit: SEARCH_LIMIT });
    if (typeFilter.value) params.set("type", typeFilter.value);
    if (seasonFilter.value) params.set("season", seasonFilter.value);
    if (yearFilter.value) params.set("year", yearFilter.value);
    if (departmentFilter.value) params.set("departmentId", departmentFilter.value);
    if (mineFilter.checked && !mineToggle.hidden) params.set("mine", "true");
    return params;
}

async function search() {
    const query = queryInput.value.trim();
    if (!query) {
        queryInput.focus();
        return;
    }
    if (!session) {
        pendingSearch = true;
        openModal("loginModal");
        return;
    }

    await withBusy(searchBtn, async () => {
        resultsMeta.textContent = "";
        resultsDiv.replaceChildren(el("p", { class: "results-note" }, "Searching…"));
        try {
            const files = await api(`/files/search?${searchParams(query)}`);
            showResults(files, query);
        } catch (error) {
            resultsDiv.replaceChildren(el("p", { class: "results-note" }, error.message));
        }
    });
}

function renderResultsMeta() {
    resultsMeta.textContent = resultCount ? `${plural(resultCount, "result")} for “${lastQuery}”` : "";
    if (!resultCount) {
        resultsDiv.replaceChildren(
            el("p", { class: "results-note" }, "No matching files. Try fewer words or clear the filters."),
        );
    }
}

function showResults(files, query) {
    lastQuery = query;
    resultCount = files.length;
    resultsDiv.replaceChildren(...files.map(fileCard));
    renderResultsMeta();
}

function canRemove(file) {
    if (session?.role === "ADMIN") return true;
    return session?.role === "LECTURER" && file.uploadedBy?.id === session.id;
}

function fileStats(file) {
    const parts = [formatSize(file.sizeBytes), plural(file.downloadCount ?? 0, "download")];
    if (file.relevance != null) parts.push(`${Math.round(file.relevance * 100)}% match`);
    return parts.filter(Boolean).join(" · ");
}

function fileCard(file) {
    const uploader = file.uploadedBy
        ? [labelOf(TITLES, file.uploadedBy.title, "short"), file.uploadedBy.name].filter(Boolean).join(" ")
        : null;
    const meta = [
        uploader,
        `${labelOf(SEASONS, file.season)} ${file.year}`,
        file.course?.code,
        file.department?.name,
    ].filter(Boolean).join(" · ");

    const stats = el("p", { class: "stats" }, fileStats(file));
    const card = el("article", { class: "card" });

    const download = el("button", { type: "button", class: "btn-accent" }, "Download");
    download.addEventListener("click", () => downloadFile(file, download, stats));

    const actions = el("div", { class: "card-actions" }, download);
    if (canRemove(file)) {
        const remove = el("button", { type: "button", class: "btn-danger" }, "Remove");
        remove.addEventListener("click", () => removeFile(file, remove, card));
        actions.append(remove);
    }

    card.append(
        el("div", { class: "card-main" },
            el("h3", {},
                file.course?.name ?? "Untitled course",
                el("span", { class: "badge" }, labelOf(FILE_TYPES, file.type))),
            el("p", { class: "meta" }, meta),
            file.description ? el("p", { class: "description" }, file.description) : null,
            el("p", { class: "filename", title: file.originalName }, file.originalName),
            stats),
        actions,
    );
    return card;
}

function filenameFrom(header) {
    if (!header) return null;
    const encoded = /filename\*\s*=\s*[^']*'[^']*'([^;]+)/i.exec(header);
    if (encoded) {
        try {
            return decodeURIComponent(encoded[1].trim().replace(/^"|"$/g, ""));
        } catch {
            // Malformed encoding; fall through to the plain parameter.
        }
    }
    const plain = /filename\s*=\s*(?:"([^"]*)"|([^;]+))/i.exec(header);
    return plain ? (plain[1] ?? plain[2]).trim() : null;
}

async function downloadFile(file, button, stats) {
    await withBusy(button, async () => {
        try {
            const response = await api(`/files/${file.id}/download`, { raw: true });
            const blob = await response.blob();
            const name = filenameFrom(response.headers.get("Content-Disposition")) || file.originalName || "download";
            const url = URL.createObjectURL(blob);
            const link = el("a", { href: url, download: name });
            document.body.append(link);
            link.click();
            link.remove();
            setTimeout(() => URL.revokeObjectURL(url), 1000);

            file.downloadCount = (file.downloadCount ?? 0) + 1;
            stats.textContent = fileStats(file);
        } catch (error) {
            report(error);
        }
    }, "Downloading…");
}

async function removeFile(file, button, card) {
    if (!confirm(`Remove "${file.originalName}" from the archive?`)) return;
    await withBusy(button, async () => {
        try {
            await api(`/files/${file.id}`, { method: "DELETE" });
            card.remove();
            resultCount--;
            renderResultsMeta();
            toast("File removed", "success");
        } catch (error) {
            report(error);
        }
    });
}

searchForm.addEventListener("submit", (event) => {
    event.preventDefault();
    search();
});

[typeFilter, seasonFilter, yearFilter, departmentFilter, mineFilter].forEach((control) => {
    control.addEventListener("change", () => {
        if (session && lastQuery && queryInput.value.trim()) search();
    });
});

async function openUpload() {
    openModal("uploadModal");
    const { courseId } = uploadForm.elements;
    const submit = submitOf(uploadForm);
    courseId.disabled = true;
    submit.disabled = true;
    try {
        const courses = await api("/courses/mine");
        setOptions(courseId, courses, { label: courseLabel });
        uploadHint.hidden = courses.length > 0;
        courseId.disabled = courses.length === 0;
        submit.disabled = courses.length === 0;
    } catch (error) {
        report(error);
    }
}

uploadForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const fields = uploadForm.elements;
    const file = fields.file.files[0];
    if (!file) return;

    const data = new FormData();
    data.append("file", file);
    data.append("courseId", fields.courseId.value);
    data.append("type", fields.type.value);
    data.append("season", fields.season.value);
    data.append("year", fields.year.value);
    const description = fields.description.value.trim();
    if (description) data.append("description", description);

    await withBusy(submitOf(uploadForm), async () => {
        try {
            const result = await api("/files/upload", { method: "POST", form: data });
            const name = result.file?.originalName ?? file.name;
            toast(result.indexed ? `Uploaded ${name}` : `Uploaded ${name}. It will show up in search shortly.`, "success");
            fields.file.value = "";
            fields.description.value = "";
            closeModal("uploadModal");
        } catch (error) {
            report(error);
        }
    }, "Uploading…");
});

const adminData = {
    faculties: [],
    departments: [],
    courses: [],
    lecturers: { items: [], totalItems: 0 },
    students: { items: [], totalItems: 0 },
    assignments: { items: [], totalItems: 0 },
    enrollments: { items: [], totalItems: 0 },
};

let refreshing = null;

function refreshAdminData() {
    refreshing ??= loadAdminData().finally(() => {
        refreshing = null;
    });
    return refreshing;
}

async function loadAdminData() {
    const page = `?size=${ADMIN_PAGE_SIZE}`;
    const [faculties, departments, courses, lecturers, students, assignments, enrollments] = await Promise.all([
        api("/faculties"),
        api("/departments"),
        api("/courses"),
        api(`/lecturers${page}`),
        api(`/students${page}`),
        api(`/course-lecturers${page}`),
        api(`/enrollments${page}`),
    ]);
    Object.assign(adminData, { faculties, departments, courses, lecturers, students, assignments, enrollments });
    renderAdmin();
}

async function refreshAfterChange() {
    try {
        await refreshAdminData();
    } catch (error) {
        report(error);
    }
}

function removeButton(label, onRemove) {
    const button = el("button", { type: "button", class: "row-remove", "aria-label": `Remove ${label}` }, "Remove");
    button.addEventListener("click", () => onRemove(button));
    return button;
}

async function removeEntry(path, button, question, done) {
    if (!confirm(question)) return;
    await withBusy(button, async () => {
        try {
            await api(path, { method: "DELETE" });
            toast(done, "success");
            await refreshAfterChange();
        } catch (error) {
            report(error);
        }
    });
}

function rowText(label, detail) {
    return el("span", { class: "row-text" },
        el("span", { class: "row-label" }, label),
        detail ? el("span", { class: "row-detail" }, detail) : null);
}

function renderCount(node, shown, total) {
    node.textContent = total > shown ? `Showing ${shown} of ${total}` : plural(total, "item");
}

// Shared behaviour of every admin form: create, optional edit through PUT,
// optional removal, and a list of what already exists.
function resourceForm(config) {
    const { form, list, count, path, noun, read, label, detail, fill, removable, empty } = config;
    const submit = submitOf(form);
    const createLabel = submit.textContent;
    const cancel = form.querySelector("[data-cancel]");
    const created = config.created ?? `${noun} created`;
    let editing = null;
    let items = [];
    let total = 0;

    function stopEditing() {
        editing = null;
        form.reset();
        submit.textContent = createLabel;
        if (cancel) cancel.hidden = true;
    }

    function reset() {
        stopEditing();
        render(items, total);
    }

    function edit(item) {
        editing = item;
        fill(form.elements, item);
        submit.textContent = "Save changes";
        cancel.hidden = false;
        render(items, total);
        form.querySelector("input, select").focus();
    }

    function row(item) {
        const text = rowText(label(item), detail?.(item));
        const id = el("span", { class: "row-id" }, `#${item.id}`);
        if (fill) {
            const button = el("button", {
                type: "button",
                class: editing?.id === item.id ? "row row-edit is-editing" : "row row-edit",
                "aria-pressed": editing?.id === item.id ? "true" : "false",
            }, text, id);
            button.addEventListener("click", () => edit(item));
            return button;
        }
        const children = [text, id];
        if (removable) {
            children.push(removeButton(label(item), (button) =>
                removeEntry(`${path}/${item.id}`, button, `Remove ${label(item)}?`, `${noun} removed`)));
        }
        return el("div", { class: "row" }, ...children);
    }

    function render(next, nextTotal = next.length) {
        items = next;
        total = nextTotal;
        if (editing && !items.some((item) => item.id === editing.id)) stopEditing();
        list.replaceChildren(...(items.length ? items.map(row) : [el("p", { class: "empty" }, empty)]));
        renderCount(count, items.length, total);
    }

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        if (rejectBlank(form)) return;
        const body = read(form.elements);
        const target = editing;

        await withBusy(submit, async () => {
            try {
                await api(target ? `${path}/${target.id}` : path, { method: target ? "PUT" : "POST", body });
                toast(target ? `${noun} updated` : created, "success");
                reset();
                await refreshAfterChange();
            } catch (error) {
                report(error);
            }
        });
    });

    cancel?.addEventListener("click", reset);

    return { render };
}

const trimmed = (field) => field.value.trim();

const resources = {
    faculty: resourceForm({
        form: $("facultyForm"), list: $("facultyList"), count: $("facultyCount"),
        path: "/faculties", noun: "Faculty", empty: "No faculties yet.",
        read: (f) => ({ name: trimmed(f.name) }),
        fill: (f, item) => { f.name.value = item.name; },
        label: (item) => item.name,
    }),
    department: resourceForm({
        form: $("departmentForm"), list: $("departmentList"), count: $("departmentCount"),
        path: "/departments", noun: "Department", empty: "No departments yet.",
        read: (f) => ({ code: trimmed(f.code), name: trimmed(f.name), facultyId: Number(f.facultyId.value) }),
        fill: (f, item) => {
            f.facultyId.value = item.facultyId;
            f.code.value = item.code;
            f.name.value = item.name;
        },
        label: (item) => item.name,
        detail: (item) => `${item.code} · ${item.facultyName}`,
    }),
    course: resourceForm({
        form: $("courseForm"), list: $("courseList"), count: $("courseCount"),
        path: "/courses", noun: "Course", empty: "No courses yet.",
        read: (f) => ({
            code: trimmed(f.code),
            name: trimmed(f.name),
            departmentId: Number(f.departmentId.value),
            description: trimmed(f.description) || null,
        }),
        fill: (f, item) => {
            f.departmentId.value = item.departmentId;
            f.code.value = item.code;
            f.name.value = item.name;
            f.description.value = item.description ?? "";
        },
        label: (item) => item.name,
        detail: (item) => `${item.code} · ${item.departmentName}`,
    }),
    lecturer: resourceForm({
        form: $("lecturerForm"), list: $("lecturerList"), count: $("lecturerCount"),
        path: "/lecturers", noun: "Lecturer", empty: "No lecturers yet.",
        read: (f) => ({
            title: f.title.value,
            firstName: trimmed(f.firstName),
            lastName: trimmed(f.lastName),
            email: trimmed(f.email),
            password: f.password.value,
            departmentId: Number(f.departmentId.value),
        }),
        label: (item) => `${labelOf(TITLES, item.title, "short")} ${personName(item)}`,
        detail: (item) => `${item.email} · ${item.departmentName}`,
    }),
    student: resourceForm({
        form: $("studentForm"), list: $("studentList"), count: $("studentCount"),
        path: "/students", noun: "Student", empty: "No students yet.",
        read: (f) => ({
            studentNumber: trimmed(f.studentNumber),
            firstName: trimmed(f.firstName),
            lastName: trimmed(f.lastName),
            email: trimmed(f.email),
            password: f.password.value,
            departmentId: Number(f.departmentId.value),
        }),
        label: (item) => personName(item),
        detail: (item) => `${item.studentNumber} · ${item.email}`,
    }),
    assignment: resourceForm({
        form: $("assignForm"), list: $("assignList"), count: $("assignCount"),
        path: "/course-lecturers", noun: "Assignment", created: "Lecturer assigned",
        empty: "No lecturers are assigned to courses yet.", removable: true,
        read: (f) => ({ courseId: Number(f.courseId.value), lecturerId: Number(f.lecturerId.value) }),
        label: (item) => item.lecturerName,
        detail: (item) => `${item.courseCode} ${item.courseName}`,
    }),
    enrollment: resourceForm({
        form: $("enrollForm"), list: $("enrollList"), count: $("enrollCount"),
        path: "/enrollments", noun: "Enrollment", created: "Student enrolled",
        empty: "No students are enrolled yet.", removable: true,
        read: (f) => ({ studentId: Number(f.studentId.value), courseId: Number(f.courseId.value) }),
        label: (item) => item.studentName,
        detail: (item) => `${item.studentNumber} · ${item.courseCode} ${item.courseName}`,
    }),
};

const aliasForm = $("aliasForm");
const aliasList = $("aliasList");

function selectedAliasCourse() {
    return adminData.courses.find((course) => String(course.id) === aliasForm.elements.courseId.value);
}

function renderAliases() {
    const course = selectedAliasCourse();
    if (!course) {
        aliasList.replaceChildren(el("p", { class: "empty" }, "Pick a course to see its aliases."));
        return;
    }
    const aliases = course.aliases ?? [];
    if (!aliases.length) {
        aliasList.replaceChildren(el("p", { class: "empty" }, "This course has no aliases yet."));
        return;
    }
    aliasList.replaceChildren(...aliases.map((alias) => el("div", { class: "row" },
        rowText(alias.alias),
        removeButton(alias.alias, (button) => removeEntry(
            `/courses/${course.id}/aliases/${alias.id}`, button,
            `Remove the alias "${alias.alias}"?`, "Alias removed")))));
}

aliasForm.elements.courseId.addEventListener("change", renderAliases);

aliasForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (rejectBlank(aliasForm)) return;
    const { courseId, alias } = aliasForm.elements;

    await withBusy(submitOf(aliasForm), async () => {
        try {
            await api(`/courses/${courseId.value}/aliases`, { method: "POST", body: { alias: trimmed(alias) } });
            alias.value = "";
            toast("Alias added", "success");
            await refreshAfterChange();
        } catch (error) {
            report(error);
        }
    });
});

function renderAdmin() {
    const { faculties, departments, courses, lecturers, students, assignments, enrollments } = adminData;
    const byName = { label: (item) => item.name };

    setOptions($("departmentForm").elements.facultyId, faculties, { placeholder: "Choose a faculty", ...byName });
    for (const form of ["courseForm", "lecturerForm", "studentForm"]) {
        setOptions($(form).elements.departmentId, departments, { placeholder: "Choose a department", ...byName });
    }
    for (const select of [aliasForm.elements.courseId, $("assignForm").elements.courseId, $("enrollForm").elements.courseId]) {
        setOptions(select, courses, { placeholder: "Choose a course", label: courseLabel });
    }
    setOptions($("assignForm").elements.lecturerId, lecturers.items, {
        placeholder: "Choose a lecturer",
        label: (item) => `${personName(item)} (${item.email})`,
    });
    setOptions($("enrollForm").elements.studentId, students.items, {
        placeholder: "Choose a student",
        label: (item) => `${personName(item)} (${item.studentNumber})`,
    });
    setOptions(departmentFilter, departments, { placeholder: "All departments", ...byName });

    resources.faculty.render(faculties);
    resources.department.render(departments);
    resources.course.render(courses);
    resources.lecturer.render(lecturers.items, lecturers.totalItems);
    resources.student.render(students.items, students.totalItems);
    resources.assignment.render(assignments.items, assignments.totalItems);
    resources.enrollment.render(enrollments.items, enrollments.totalItems);
    renderAliases();
}

const tabs = [...document.querySelectorAll('[role="tab"]')];
const indexStatus = $("indexStatus");
let activeTab = "faculty";
let indexTimer = null;

function selectTab(name, focus = false) {
    activeTab = name;
    for (const tab of tabs) {
        const selected = tab.dataset.tab === name;
        tab.setAttribute("aria-selected", String(selected));
        tab.classList.toggle("active", selected);
        tab.tabIndex = selected ? 0 : -1;
        $(tab.getAttribute("aria-controls")).hidden = !selected;
        if (selected && focus) tab.focus();
    }
    clearTimeout(indexTimer);
    if (name === "index") loadIndexStatus();
}

tabs.forEach((tab, index) => {
    tab.addEventListener("click", () => selectTab(tab.dataset.tab));
    tab.addEventListener("keydown", (event) => {
        const step = { ArrowRight: 1, ArrowLeft: -1 }[event.key];
        if (!step) return;
        event.preventDefault();
        selectTab(tabs[(index + step + tabs.length) % tabs.length].dataset.tab, true);
    });
});

function indexPollingWanted() {
    return $("adminModal").classList.contains("open") && activeTab === "index";
}

function renderIndexStatus(status) {
    if (!status.startedAt) {
        indexStatus.replaceChildren(el("p", { class: "hint" }, "No indexing job has run since the server started."));
        return;
    }
    const heading = status.running
        ? `Indexing ${status.processed} of ${status.total} files…`
        : `Last run finished ${formatTime(status.finishedAt)}`;
    const summary = [
        `${status.processed} of ${plural(status.total, "file")} processed`,
        status.failed ? `${status.failed} failed` : null,
        `started ${formatTime(status.startedAt)}`,
    ].filter(Boolean).join(" · ");

    indexStatus.replaceChildren(
        el("strong", {}, heading),
        el("progress", { max: Math.max(status.total, 1), value: status.processed }),
        el("p", { class: "hint" }, summary),
    );
}

async function loadIndexStatus() {
    clearTimeout(indexTimer);
    try {
        const status = await api("/files/index/status");
        renderIndexStatus(status);
        if (status.running && indexPollingWanted()) indexTimer = setTimeout(loadIndexStatus, INDEX_POLL_MS);
    } catch (error) {
        indexStatus.replaceChildren(el("p", { class: "hint" }, error.message));
    }
}

async function startIndexing(all, button) {
    if (all && !confirm("Re-index every file? This can take a while for a large archive.")) return;
    await withBusy(button, async () => {
        try {
            const result = await api(`/files/index${all ? "?all=true" : ""}`, { method: "POST" });
            const fallback = result?.started ? "Indexing started" : "An indexing job is already running";
            toast(result?.message || fallback, result?.started ? "success" : "info");
            await loadIndexStatus();
        } catch (error) {
            report(error);
        }
    });
}

$("indexMissingBtn").addEventListener("click", (event) => startIndexing(false, event.currentTarget));
$("indexAllBtn").addEventListener("click", (event) => startIndexing(true, event.currentTarget));

async function openAdmin() {
    openModal("adminModal");
    try {
        await refreshAdminData();
    } catch (error) {
        report(error);
    }
    if (activeTab === "index") loadIndexStatus();
}

$("adminModal").addEventListener("modalclose", () => clearTimeout(indexTimer));

openLoginBtn.addEventListener("click", () => openModal("loginModal"));
openUploadBtn.addEventListener("click", openUpload);
openAdminBtn.addEventListener("click", openAdmin);
openPasswordBtn.addEventListener("click", () => openModal("passwordModal"));
logoutBtn.addEventListener("click", logout);
$("loginModal").addEventListener("modalclose", () => {
    pendingSearch = false;
});

window.addEventListener("storage", (event) => {
    if (event.key !== SESSION_KEY && event.key !== null) return;
    session = readSession();
    scheduleExpiry();
    if (!session) closeAllModals();
    updateAuthUI();
});

function init() {
    setOptions(typeFilter, FILE_TYPES, enumOptions("All types"));
    setOptions(seasonFilter, SEASONS, enumOptions("All seasons"));
    const years = Array.from({ length: 10 }, (_, i) => currentYear - i);
    setOptions(yearFilter, years, { placeholder: "All years", value: String, label: String });
    setOptions(departmentFilter, [], { placeholder: "All departments", label: String });

    const fields = uploadForm.elements;
    setOptions(fields.type, FILE_TYPES, enumOptions());
    setOptions(fields.season, SEASONS, enumOptions());
    fields.year.min = 2000;
    fields.year.max = currentYear + 1;
    fields.year.value = currentYear;
    fields.year.defaultValue = currentYear;

    setOptions($("lecturerForm").elements.title, TITLES, enumOptions());

    updateAuthUI();
    scheduleExpiry();
    if (session) loadDepartmentFilter();
}

init();
