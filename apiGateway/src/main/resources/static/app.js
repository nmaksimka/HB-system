const state = {
  users: [],
  groups: [],
  reminders: [],
  currentUserId: localStorage.getItem("birthdayHub.currentUserId") || "",
  selectedUserId: "",
  chatSocket: null,
  chatConnected: false,
  stompSubscriptionId: 0,
};

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => Array.from(document.querySelectorAll(selector));

const els = {
  currentUserSelect: $("#currentUserSelect"),
  createUserForm: $("#createUserForm"),
  refreshAllButton: $("#refreshAllButton"),
  refreshRemindersButton: $("#refreshRemindersButton"),
  reminderDays: $("#reminderDays"),
  usersMetric: $("#usersMetric"),
  groupsMetric: $("#groupsMetric"),
  remindersMetric: $("#remindersMetric"),
  remindersList: $("#remindersList"),
  peopleSearch: $("#peopleSearch"),
  usersList: $("#usersList"),
  friendCardEmpty: $("#friendCardEmpty"),
  friendCardContent: $("#friendCardContent"),
  friendName: $("#friendName"),
  friendBirthDate: $("#friendBirthDate"),
  friendGroups: $("#friendGroups"),
  friendWishes: $("#friendWishes"),
  subscribeFriendButton: $("#subscribeFriendButton"),
  chatStatus: $("#chatStatus"),
  chatMessages: $("#chatMessages"),
  chatForm: $("#chatForm"),
  groupsList: $("#groupsList"),
  createGroupForm: $("#createGroupForm"),
  myWishesList: $("#myWishesList"),
  createWishForm: $("#createWishForm"),
  toast: $("#toast"),
};

function currentUser() {
  return state.users.find((user) => user.id === state.currentUserId) || null;
}

function selectedUser() {
  return state.users.find((user) => user.id === state.selectedUserId) || null;
}

function api(path, options = {}) {
  return fetch(path, {
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {}),
    },
    ...options,
  }).then(async (response) => {
    if (response.status === 204) return null;
    const text = await response.text();
    const data = text ? JSON.parse(text) : null;
    if (!response.ok) {
      const detail = data?.detail || data?.title || response.statusText;
      throw new Error(detail);
    }
    return data;
  });
}

function formatDate(value) {
  if (!value) return "Дата не указана";
  return new Intl.DateTimeFormat("ru-RU", {
    day: "numeric",
    month: "long",
    year: "numeric",
  }).format(new Date(`${value}T00:00:00`));
}

function formatDateTime(value) {
  if (!value) return "";
  return new Intl.DateTimeFormat("ru-RU", {
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

function daysUntilBirthday(birthDate) {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const [year, month, day] = birthDate.split("-").map(Number);
  let next = new Date(today.getFullYear(), month - 1, day);
  if (next < today) next = new Date(today.getFullYear() + 1, month - 1, day);
  if (month === 2 && day === 29 && next.getMonth() !== 1) {
    next = new Date(next.getFullYear(), 1, 28);
  }
  return Math.round((next - today) / 86400000);
}

function showToast(message) {
  els.toast.textContent = message;
  els.toast.classList.add("visible");
  window.clearTimeout(showToast.timer);
  showToast.timer = window.setTimeout(() => els.toast.classList.remove("visible"), 3200);
}

function setSection(sectionId) {
  $$("[data-section]").forEach((section) => {
    section.classList.toggle("active", section.dataset.section === sectionId);
  });
  $$("[data-section-link]").forEach((link) => {
    link.classList.toggle("active", link.dataset.sectionLink === sectionId);
  });
}

async function loadCoreData() {
  const [users, groups] = await Promise.all([api("/api/users"), api("/api/groups")]);
  state.users = users.sort((a, b) => daysUntilBirthday(a.birthDate) - daysUntilBirthday(b.birthDate));
  state.groups = groups.sort((a, b) => a.name.localeCompare(b.name, "ru"));
  if (!state.currentUserId && state.users[0]) state.currentUserId = state.users[0].id;
  if (!state.selectedUserId && state.users[0]) {
    state.selectedUserId = state.users.find((user) => user.id !== state.currentUserId)?.id || state.users[0].id;
  }
  localStorage.setItem("birthdayHub.currentUserId", state.currentUserId);
  renderAll();
  await loadCurrentUserWishes();
}

async function refreshReminders() {
  if (!state.currentUserId) return;
  const days = Number(els.reminderDays.value || 30);
  state.reminders = await api(`/api/notifications/users/${state.currentUserId}/refresh?days=${days}`, {
    method: "POST",
  });
  renderReminders();
  renderMetrics();
}

function renderAll() {
  renderCurrentUserSelect();
  renderMetrics();
  renderUsers();
  renderGroups();
  renderFriendCard();
}

function renderCurrentUserSelect() {
  els.currentUserSelect.innerHTML = state.users
    .map((user) => `<option value="${user.id}">${escapeHtml(user.name)}</option>`)
    .join("");
  els.currentUserSelect.value = state.currentUserId;
}

function renderMetrics() {
  els.usersMetric.textContent = state.users.length;
  els.groupsMetric.textContent = state.groups.length;
  els.remindersMetric.textContent = state.reminders.filter((reminder) => !reminder.readAt).length;
}

function renderUsers() {
  const query = els.peopleSearch.value.trim().toLowerCase();
  const users = state.users.filter((user) => user.name.toLowerCase().includes(query));
  els.usersList.innerHTML = users.map((user) => {
    const days = daysUntilBirthday(user.birthDate);
    return `
      <article class="person-row ${user.id === state.selectedUserId ? "active" : ""}">
        <div>
          <strong>${escapeHtml(user.name)}</strong>
          <span class="muted">${formatDate(user.birthDate)} · через ${days} дн.</span>
        </div>
        <div class="row-actions">
          <button type="button" data-open-user="${user.id}">Открыть</button>
        </div>
      </article>
    `;
  }).join("") || empty("Пользователей пока нет");
}

async function renderFriendCard() {
  const user = selectedUser();
  if (!user) {
    els.friendCardEmpty.classList.remove("hidden");
    els.friendCardContent.classList.add("hidden");
    return;
  }

  els.friendCardEmpty.classList.add("hidden");
  els.friendCardContent.classList.remove("hidden");
  els.friendName.textContent = user.name;
  els.friendBirthDate.textContent = `${formatDate(user.birthDate)} · ближайший день рождения через ${daysUntilBirthday(user.birthDate)} дн.`;
  els.subscribeFriendButton.disabled = user.id === state.currentUserId;

  try {
    const card = await api(`/api/users/${user.id}/card`);
    els.friendGroups.innerHTML = card.groups.length
      ? card.groups.map((group) => `<span class="chip">${escapeHtml(group)}</span>`).join("")
      : `<span class="muted">Пока не состоит в группах.</span>`;
    els.friendWishes.innerHTML = renderWishes(card.wishes);
  } catch (error) {
    els.friendGroups.innerHTML = "";
    els.friendWishes.innerHTML = empty(error.message);
  }

  await loadChat();
}

function renderGroups() {
  els.groupsList.innerHTML = state.groups.map((group) => `
    <article class="group-card">
      <strong>${escapeHtml(group.name)}</strong>
      <span class="muted">Создатель: ${shortId(group.ownerId)}</span>
      <div class="row-actions">
        <button type="button" data-join-group="${group.id}">Вступить</button>
        <button type="button" data-subscribe-group="${group.id}" class="secondary">Подписаться</button>
      </div>
    </article>
  `).join("") || empty("Групп пока нет");
}

async function loadCurrentUserWishes() {
  if (!state.currentUserId) {
    els.myWishesList.innerHTML = empty("Сначала создайте или выберите пользователя");
    return;
  }
  const wishes = await api(`/api/users/${state.currentUserId}/wishes`);
  els.myWishesList.innerHTML = renderWishes(wishes, true);
}

function renderWishes(wishes, editable = false) {
  if (!wishes?.length) return empty("Список желаний пока пуст");
  return wishes.map((wish) => `
    <article class="wish-card">
      <strong>${escapeHtml(wish.title)}</strong>
      <span class="muted">Приоритет: ${wish.priority}</span>
      ${wish.description ? `<p>${escapeHtml(wish.description)}</p>` : ""}
      ${wish.link ? `<a href="${escapeAttribute(wish.link)}" target="_blank" rel="noreferrer">Открыть ссылку</a>` : ""}
      ${editable ? `<div class="row-actions"><button class="danger" type="button" data-delete-wish="${wish.id}">Удалить</button></div>` : ""}
    </article>
  `).join("");
}

function renderReminders() {
  els.remindersList.innerHTML = state.reminders.map((reminder) => `
    <article class="reminder-card">
      <strong>${escapeHtml(reminder.birthdayUserName)}</strong>
      <span class="muted">${formatDate(reminder.birthdayDate)}</span>
      <p class="${reminder.readAt ? "status-read" : "status-unread"}">
        ${reminder.readAt ? "Прочитано" : "Новое напоминание"}
      </p>
      <div class="row-actions">
        <button type="button" data-open-user="${reminder.birthdayUserId}">Карточка</button>
        ${reminder.readAt ? "" : `<button type="button" class="secondary" data-read-reminder="${reminder.id}">Отметить</button>`}
      </div>
    </article>
  `).join("") || empty("Нет напоминаний. Подпишитесь на друга или группу.");
}

async function subscribe(targetType, targetId) {
  if (!state.currentUserId) throw new Error("Выберите текущего пользователя");
  await api("/api/subscriptions", {
    method: "POST",
    body: JSON.stringify({
      subscriberId: state.currentUserId,
      targetType,
      targetId,
    }),
  });
  showToast(targetType === "USER" ? "Подписка на друга создана" : "Подписка на группу создана");
}

async function loadChat() {
  closeChatSocket();
  els.chatMessages.innerHTML = "";
  const friend = selectedUser();
  const me = currentUser();
  const disabled = !friend || !me || friend.id === me.id;
  els.chatForm.text.disabled = disabled;
  els.chatForm.querySelector("button").disabled = disabled;
  if (disabled) {
    els.chatStatus.textContent = "Именинник не может читать собственный секретный чат.";
    els.chatMessages.innerHTML = empty("Выберите другого пользователя как текущего участника.");
    return;
  }

  try {
    const messages = await api(`/api/chat/users/${friend.id}/messages?viewerId=${me.id}`);
    renderMessages(messages);
    connectChatSocket(friend.id);
  } catch (error) {
    els.chatStatus.textContent = error.message;
    els.chatMessages.innerHTML = empty("Чат недоступен");
  }
}

function renderMessages(messages) {
  els.chatMessages.innerHTML = messages.map(messageTemplate).join("") || empty("Сообщений пока нет");
  els.chatMessages.scrollTop = els.chatMessages.scrollHeight;
}

function appendMessage(message) {
  const emptyState = els.chatMessages.querySelector(".empty-inline");
  if (emptyState) els.chatMessages.innerHTML = "";
  els.chatMessages.insertAdjacentHTML("beforeend", messageTemplate(message));
  els.chatMessages.scrollTop = els.chatMessages.scrollHeight;
}

function messageTemplate(message) {
  const author = state.users.find((user) => user.id === message.authorId);
  const own = message.authorId === state.currentUserId;
  return `
    <article class="message ${own ? "own" : ""}">
      <div class="message-meta">${escapeHtml(author?.name || shortId(message.authorId))} · ${formatDateTime(message.createdAt)}</div>
      <div>${escapeHtml(message.text)}</div>
    </article>
  `;
}

function connectChatSocket(subjectUserId) {
  const protocol = window.location.protocol === "https:" ? "wss" : "ws";
  const socket = new WebSocket(`${protocol}://${window.location.host}/ws`);
  state.chatSocket = socket;
  state.chatConnected = false;
  els.chatStatus.textContent = "Подключаем real-time чат...";

  socket.addEventListener("open", () => {
    socket.send("CONNECT\naccept-version:1.2\nheart-beat:10000,10000\n\n\0");
  });

  socket.addEventListener("message", (event) => {
    String(event.data).split("\0").filter(Boolean).forEach((frame) => handleStompFrame(frame, subjectUserId));
  });

  socket.addEventListener("close", () => {
    state.chatConnected = false;
    if (state.selectedUserId === subjectUserId) {
      els.chatStatus.textContent = "Real-time отключен, отправка пойдет через REST.";
    }
  });

  socket.addEventListener("error", () => {
    els.chatStatus.textContent = "WebSocket недоступен, используем REST.";
  });
}

function handleStompFrame(frame, subjectUserId) {
  const [head, body = ""] = frame.split("\n\n");
  const command = head.split("\n")[0];
  if (command === "CONNECTED") {
    state.chatConnected = true;
    const subId = `chat-${state.stompSubscriptionId++}`;
    state.chatSocket.send(`SUBSCRIBE\nid:${subId}\ndestination:/topic/chat/${subjectUserId}\n\n\0`);
    els.chatStatus.textContent = "Real-time чат подключен.";
  }
  if (command === "MESSAGE") {
    appendMessage(JSON.parse(body));
  }
  if (command === "ERROR") {
    els.chatStatus.textContent = "WebSocket вернул ошибку, используем REST.";
  }
}

function sendChatViaSocket(subjectUserId, payload) {
  const body = JSON.stringify(payload);
  state.chatSocket.send(`SEND\ndestination:/app/chat/${subjectUserId}\ncontent-type:application/json\n\n${body}\0`);
}

function closeChatSocket() {
  if (state.chatSocket) {
    try {
      state.chatSocket.close();
    } catch {
      // Socket is best-effort UI plumbing.
    }
  }
  state.chatSocket = null;
  state.chatConnected = false;
}

function empty(text) {
  return `<div class="empty-inline muted">${escapeHtml(text)}</div>`;
}

function shortId(id) {
  return id ? id.slice(0, 8) : "";
}

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, (char) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    "\"": "&quot;",
    "'": "&#039;",
  })[char]);
}

function escapeAttribute(value) {
  return escapeHtml(value).replace(/`/g, "&#096;");
}

function serializeForm(form) {
  return Object.fromEntries(new FormData(form).entries());
}

els.currentUserSelect.addEventListener("change", async (event) => {
  state.currentUserId = event.target.value;
  localStorage.setItem("birthdayHub.currentUserId", state.currentUserId);
  renderFriendCard();
  await Promise.all([refreshReminders(), loadCurrentUserWishes()]);
});

els.refreshAllButton.addEventListener("click", async () => {
  await loadCoreData();
  await refreshReminders();
  showToast("Данные обновлены");
});

els.refreshRemindersButton.addEventListener("click", async () => {
  await refreshReminders();
  showToast("Напоминания обновлены");
});

els.peopleSearch.addEventListener("input", renderUsers);

document.addEventListener("click", async (event) => {
  const target = event.target;
  const openUserId = target.dataset?.openUser;
  const joinGroupId = target.dataset?.joinGroup;
  const subscribeGroupId = target.dataset?.subscribeGroup;
  const deleteWishId = target.dataset?.deleteWish;
  const readReminderId = target.dataset?.readReminder;

  try {
    if (openUserId) {
      state.selectedUserId = openUserId;
      setSection("people");
      renderUsers();
      await renderFriendCard();
    }
    if (joinGroupId) {
      await api(`/api/groups/${joinGroupId}/members/${state.currentUserId}`, { method: "PUT" });
      showToast("Вы вступили в группу");
      await renderFriendCard();
    }
    if (subscribeGroupId) {
      await subscribe("GROUP", subscribeGroupId);
      await refreshReminders();
    }
    if (deleteWishId) {
      await api(`/api/users/${state.currentUserId}/wishes/${deleteWishId}`, { method: "DELETE" });
      await loadCurrentUserWishes();
      if (state.selectedUserId === state.currentUserId) await renderFriendCard();
      showToast("Желание удалено");
    }
    if (readReminderId) {
      await api(`/api/notifications/users/${state.currentUserId}/${readReminderId}/read`, { method: "PUT" });
      await refreshReminders();
    }
  } catch (error) {
    showToast(error.message);
  }
});

$$("[data-section-link]").forEach((link) => {
  link.addEventListener("click", (event) => {
    event.preventDefault();
    setSection(link.dataset.sectionLink);
  });
});

els.subscribeFriendButton.addEventListener("click", async () => {
  try {
    await subscribe("USER", state.selectedUserId);
    await refreshReminders();
  } catch (error) {
    showToast(error.message);
  }
});

els.createUserForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    await api("/api/users", {
      method: "POST",
      body: JSON.stringify(serializeForm(event.currentTarget)),
    });
    event.currentTarget.reset();
    await loadCoreData();
    showToast("Пользователь создан");
  } catch (error) {
    showToast(error.message);
  }
});

els.createGroupForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    const body = serializeForm(event.currentTarget);
    body.ownerId = state.currentUserId;
    await api("/api/groups", {
      method: "POST",
      body: JSON.stringify(body),
    });
    event.currentTarget.reset();
    await loadCoreData();
    showToast("Группа создана");
  } catch (error) {
    showToast(error.message);
  }
});

els.createWishForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    const body = serializeForm(event.currentTarget);
    body.priority = Number(body.priority);
    await api(`/api/users/${state.currentUserId}/wishes`, {
      method: "POST",
      body: JSON.stringify(body),
    });
    event.currentTarget.reset();
    event.currentTarget.priority.value = 1;
    await loadCurrentUserWishes();
    if (state.selectedUserId === state.currentUserId) await renderFriendCard();
    showToast("Желание опубликовано");
  } catch (error) {
    showToast(error.message);
  }
});

els.chatForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const text = event.currentTarget.text.value.trim();
  if (!text) return;
  const payload = { authorId: state.currentUserId, text };
  try {
    if (state.chatConnected && state.chatSocket?.readyState === WebSocket.OPEN) {
      sendChatViaSocket(state.selectedUserId, payload);
    } else {
      const message = await api(`/api/chat/users/${state.selectedUserId}/messages`, {
        method: "POST",
        body: JSON.stringify(payload),
      });
      appendMessage(message);
    }
    event.currentTarget.reset();
  } catch (error) {
    showToast(error.message);
  }
});

window.addEventListener("beforeunload", closeChatSocket);

loadCoreData()
  .then(refreshReminders)
  .catch((error) => {
    showToast(error.message);
    els.usersList.innerHTML = empty("Не удалось загрузить API. Проверьте, что gateway запущен.");
  });
