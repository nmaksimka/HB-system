const authConfig = {
  issuer: "http://localhost:8080/realms/birthday-hub",
  clientId: "birthday-hub-ui",
  redirectUri: `${window.location.origin}/`,
};

const storageKeys = {
  accessToken: "birthdayHub.accessToken",
  refreshToken: "birthdayHub.refreshToken",
  idToken: "birthdayHub.idToken",
  expiresAt: "birthdayHub.expiresAt",
};

const state = {
  me: null,
  users: [],
  groups: [],
  groupMembers: {},
  subscriptions: [],
  reminders: [],
  selectedUserId: "",
  chatSocket: null,
  chatConnected: false,
  stompSubscriptionId: 0,
};

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => Array.from(document.querySelectorAll(selector));

const els = {
  authView: $("#authView"),
  appView: $("#appView"),
  loginForm: $("#loginForm"),
  registerForm: $("#registerForm"),
  logoutButton: $("#logoutButton"),
  currentUserLabel: $("#currentUserLabel"),
  pageTitle: $("#pageTitle"),
  profileSetup: $("#profileSetup"),
  profileForm: $("#profileForm"),
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

function tokenEndpoint() {
  return `${authConfig.issuer}/protocol/openid-connect/token`;
}

function getAccessToken() {
  return localStorage.getItem(storageKeys.accessToken) || "";
}

function hasSession() {
  return Boolean(getAccessToken());
}

function saveTokens(tokens) {
  const now = Date.now();
  const expiresIn = Number(tokens.expires_in || 300);
  localStorage.setItem(storageKeys.accessToken, tokens.access_token);
  if (tokens.refresh_token) localStorage.setItem(storageKeys.refreshToken, tokens.refresh_token);
  if (tokens.id_token) localStorage.setItem(storageKeys.idToken, tokens.id_token);
  localStorage.setItem(storageKeys.expiresAt, String(now + expiresIn * 1000));
}

function clearSession() {
  closeChatSocket();
  Object.values(storageKeys).forEach((key) => {
    localStorage.removeItem(key);
    sessionStorage.removeItem(key);
  });
  state.me = null;
  state.users = [];
  state.groups = [];
  state.groupMembers = {};
  state.subscriptions = [];
  state.reminders = [];
  state.selectedUserId = "";
}

async function tokenRequest(body) {
  const response = await fetch(tokenEndpoint(), {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams(body),
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(data.error_description || data.error || "Не удалось войти");
  }
  saveTokens(data);
  return data;
}

async function loginWithPassword(username, password) {
  await tokenRequest({
    grant_type: "password",
    client_id: authConfig.clientId,
    username,
    password,
    scope: "openid profile",
  });
}

async function registerAccount(payload) {
  const response = await fetch("/api/auth/register", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  const text = await response.text();
  const data = text ? JSON.parse(text) : null;
  if (!response.ok) {
    const detail = data?.detail || data?.title || response.statusText;
    throw new Error(detail);
  }
  return data;
}

async function refreshTokenIfNeeded(force = false) {
  const refreshToken = localStorage.getItem(storageKeys.refreshToken);
  const expiresAt = Number(localStorage.getItem(storageKeys.expiresAt) || 0);
  if (!refreshToken || (!force && Date.now() < expiresAt - 45000)) return;
  await tokenRequest({
    grant_type: "refresh_token",
    client_id: authConfig.clientId,
    refresh_token: refreshToken,
  });
}

async function api(path, options = {}, retry = true) {
  await refreshTokenIfNeeded();
  const response = await fetch(path, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${getAccessToken()}`,
      ...(options.headers || {}),
    },
  });

  if (response.status === 401 && retry) {
    try {
      await refreshTokenIfNeeded(true);
      return api(path, options, false);
    } catch {
      clearSession();
      showAuth();
      throw new Error("Сессия истекла. Войдите заново.");
    }
  }

  if (response.status === 204) return null;
  const text = await response.text();
  const data = text ? JSON.parse(text) : null;
  if (!response.ok) {
    const detail = data?.detail || data?.title || response.statusText;
    const error = new Error(detail);
    error.status = response.status;
    throw error;
  }
  return data;
}

function showAuth() {
  els.authView.classList.remove("hidden");
  els.appView.classList.add("hidden");
}

function showApp() {
  els.authView.classList.add("hidden");
  els.appView.classList.remove("hidden");
}

function selectedUser() {
  return state.users.find((user) => user.id === state.selectedUserId) || null;
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
  if (!birthDate) return 0;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const [, month, day] = birthDate.split("-").map(Number);
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

function setLoading(button, loadingText) {
  const original = button.textContent;
  button.disabled = true;
  button.textContent = loadingText;
  return () => {
    button.disabled = false;
    button.textContent = original;
  };
}

function setSection(sectionId) {
  const titles = {
    dashboard: "Кого поздравляем следующим?",
    people: "Друзья и идеи подарков",
    groups: "Группы поздравлений",
    wishes: "Мой wishlist",
  };
  els.pageTitle.textContent = titles[sectionId] || titles.dashboard;
  $$("[data-section]").forEach((section) => {
    section.classList.toggle("active", section.dataset.section === sectionId);
  });
  $$("[data-section-link]").forEach((link) => {
    link.classList.toggle("active", link.dataset.sectionLink === sectionId);
  });
}

async function bootstrap() {
  showApp();
  try {
    state.me = await api("/api/me");
    els.profileSetup.classList.add("hidden");
    await loadCoreData();
    await Promise.allSettled([refreshReminders(), loadCurrentUserWishes()]);
  } catch (error) {
    if (error.status === 404) {
      state.me = null;
      els.currentUserLabel.textContent = "Новый аккаунт";
      els.profileSetup.classList.remove("hidden");
      await loadPublicLists();
      renderAll();
      showToast("Завершите профиль, чтобы пользоваться приложением.");
      return;
    }
    showToast(error.message);
    if (error.status === 401) {
      clearSession();
      showAuth();
    }
  }
}

async function loadPublicLists() {
  const [users, groups] = await Promise.all([api("/api/users"), api("/api/groups")]);
  state.users = sortUsers(users);
  state.groups = groups.sort((a, b) => a.name.localeCompare(b.name, "ru"));
}

async function loadCoreData() {
  const [users, groups, subscriptions] = await Promise.all([
    api("/api/users"),
    api("/api/groups"),
    api("/api/subscriptions"),
  ]);
  state.users = sortUsers(users);
  state.groups = groups.sort((a, b) => a.name.localeCompare(b.name, "ru"));
  state.subscriptions = subscriptions;
  await loadGroupMembers();
  const friends = friendUsers();
  if (!state.selectedUserId || !friends.some((user) => user.id === state.selectedUserId)) {
    state.selectedUserId = friends[0]?.id || "";
  }
  renderAll();
}

function sortUsers(users) {
  return users.slice().sort((a, b) => daysUntilBirthday(a.birthDate) - daysUntilBirthday(b.birthDate));
}

function friendIds() {
  return new Set(
    state.subscriptions
      .filter((subscription) => subscription.targetType === "USER")
      .map((subscription) => subscription.targetId)
  );
}

function isFriend(userId) {
  return friendIds().has(userId);
}

function friendUsers() {
  const ids = friendIds();
  return state.users.filter((user) => ids.has(user.id));
}

function groupSubscriptionIds() {
  return new Set(
    state.subscriptions
      .filter((subscription) => subscription.targetType === "GROUP")
      .map((subscription) => subscription.targetId)
  );
}

function isGroupSubscribed(groupId) {
  return groupSubscriptionIds().has(groupId);
}

function isGroupMember(groupId) {
  return Boolean(state.groupMembers[groupId]?.some((user) => user.id === state.me?.id));
}

async function loadGroupMembers() {
  const entries = await Promise.all(
    state.groups.map(async (group) => {
      try {
        return [group.id, await api(`/api/groups/${group.id}/members`)];
      } catch {
        return [group.id, []];
      }
    })
  );
  state.groupMembers = Object.fromEntries(entries);
}

async function refreshReminders() {
  if (!state.me) return;
  const days = Number(els.reminderDays.value || 30);
  state.reminders = await api(`/api/notifications/refresh?days=${days}`, { method: "POST" });
  renderReminders();
  renderMetrics();
}

function renderAll() {
  renderIdentity();
  renderMetrics();
  renderUsers();
  renderGroups();
  renderFriendCard();
}

function renderIdentity() {
  els.currentUserLabel.textContent = state.me?.name || "Профиль не заполнен";
}

function renderMetrics() {
  els.usersMetric.textContent = friendUsers().length;
  els.groupsMetric.textContent = state.groups.length;
  els.remindersMetric.textContent = state.reminders.filter((reminder) => !reminder.readAt).length;
}

function renderUsers() {
  const query = els.peopleSearch.value.trim().toLowerCase();
  const friends = friendUsers().filter((user) => user.name.toLowerCase().includes(query));
  const candidates = state.users
    .filter((user) => user.id !== state.me?.id && !isFriend(user.id))
    .filter((user) => user.name.toLowerCase().includes(query));

  els.usersList.innerHTML = `
    <section class="list-section">
      <div class="list-section-title">
        <span>Мои друзья</span>
        <span class="count-pill">${friends.length}</span>
      </div>
      ${friends.map(friendRow).join("") || empty("Вы пока никого не добавили. Найдите человека ниже.")}
    </section>
    <section class="list-section">
      <div class="list-section-title">
        <span>Найти людей</span>
        <span class="count-pill">${candidates.length}</span>
      </div>
      ${candidates.map(candidateRow).join("") || empty("Новых людей по запросу не найдено")}
    </section>
  `;
}

function friendRow(user) {
    const days = daysUntilBirthday(user.birthDate);
    return `
      <article class="person-row ${user.id === state.selectedUserId ? "active" : ""}">
        <div>
          <strong>${escapeHtml(user.name)}</strong>
          <span class="muted">${formatDate(user.birthDate)} · через ${days} дн.</span>
        </div>
        <div class="row-actions">
          <button type="button" data-open-user="${user.id}">Открыть</button>
          <button type="button" class="danger" data-remove-friend="${user.id}">Удалить</button>
        </div>
      </article>
    `;
}

function candidateRow(user) {
  const days = daysUntilBirthday(user.birthDate);
  return `
    <article class="person-row locked">
      <div>
        <strong>${escapeHtml(user.name)}</strong>
        <span class="muted">${formatDate(user.birthDate)} · через ${days} дн.</span>
      </div>
      <div class="row-actions">
        <button type="button" data-open-user="${user.id}" class="secondary">Посмотреть</button>
        <button type="button" data-add-friend="${user.id}">Добавить</button>
      </div>
    </article>
  `;
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
  els.subscribeFriendButton.disabled = !state.me || user.id === state.me.id;
  els.subscribeFriendButton.textContent = isFriend(user.id) ? "Удалить из друзей" : "Добавить друга";
  els.subscribeFriendButton.classList.toggle("danger", isFriend(user.id));

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
  els.groupsList.innerHTML = state.groups.map((group) => {
    const members = state.groupMembers[group.id] || [];
    const subscribed = isGroupSubscribed(group.id);
    const member = isGroupMember(group.id);
    return `
      <article class="group-card">
        <strong>${escapeHtml(group.name)}</strong>
        <span class="muted">Владелец: ${ownerName(group.ownerId)} · участников: ${members.length}</span>
        <div class="member-list">
          ${members.map((user) => `<span class="member-chip">${escapeHtml(user.name)}</span>`).join("") || `<span class="muted">Участников пока нет</span>`}
        </div>
        <div class="row-actions">
          ${member
            ? `<button type="button" class="danger" data-leave-group="${group.id}">Выйти</button>`
            : `<button type="button" data-join-group="${group.id}" ${state.me ? "" : "disabled"}>Вступить</button>`}
          ${subscribed
            ? `<button type="button" class="danger" data-unsubscribe-group="${group.id}">Отписаться</button>`
            : `<button type="button" data-subscribe-group="${group.id}" class="secondary" ${state.me ? "" : "disabled"}>Подписаться на группу</button>`}
        </div>
      </article>
    `;
  }).join("") || empty("Групп пока нет");
}

async function loadCurrentUserWishes() {
  if (!state.me) {
    els.myWishesList.innerHTML = empty("Заполните профиль, чтобы добавить wishlist.");
    return;
  }
  const wishes = await api("/api/me/wishes");
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
  `).join("") || empty("Напоминаний пока нет. Подпишитесь на друга или группу.");
}

async function subscribe(targetType, targetId) {
  if (!state.me) throw new Error("Сначала завершите профиль");
  const targetPath = targetType === "USER" ? "users" : "groups";
  await api(`/api/subscriptions/${targetPath}/${targetId}`, { method: "POST" });
  showToast(targetType === "USER" ? "Друг добавлен" : "Подписка на группу создана");
}

async function unsubscribe(targetType, targetId) {
  if (!state.me) throw new Error("Сначала завершите профиль");
  const targetPath = targetType === "USER" ? "users" : "groups";
  await api(`/api/subscriptions/${targetPath}/${targetId}`, { method: "DELETE" });
}

async function subscribeGroupAndMembers(groupId) {
  await subscribe("GROUP", groupId);
  showToast("Вы подписались на группу и всех ее участников");
}

async function loadChat() {
  closeChatSocket();
  els.chatMessages.innerHTML = "";
  const friend = selectedUser();
  const disabled = !friend || !state.me || friend.id === state.me.id || !isFriend(friend.id);
  els.chatForm.text.disabled = disabled;
  els.chatForm.querySelector("button").disabled = disabled;
  if (disabled) {
    els.chatStatus.textContent = !state.me
      ? "Заполните профиль, чтобы пользоваться чатом."
      : friend?.id === state.me.id
        ? "Именинник не может читать собственный секретный чат."
        : "Добавьте человека в друзья, чтобы открыть секретный чат.";
    els.chatMessages.innerHTML = empty("Чат доступен только с добавленными друзьями.");
    return;
  }

  try {
    const messages = await api(`/api/chat/users/${friend.id}/messages`);
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
  const own = message.authorId === state.me?.id;
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
    socket.send(`CONNECT\nauthorization:Bearer ${getAccessToken()}\naccept-version:1.2\nheart-beat:10000,10000\n\n\0`);
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
  state.chatSocket.send(`SEND\ndestination:/app/chat/${subjectUserId}\nauthorization:Bearer ${getAccessToken()}\ncontent-type:application/json\n\n${body}\0`);
}

function closeChatSocket() {
  if (state.chatSocket) {
    try {
      state.chatSocket.close();
    } catch {
      // Closing is best-effort during page changes and logout.
    }
  }
  state.chatSocket = null;
  state.chatConnected = false;
}

function ownerName(id) {
  return state.users.find((user) => user.id === id)?.name || shortId(id);
}

function empty(text) {
  return `<div class="empty-inline">${escapeHtml(text)}</div>`;
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

els.loginForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  const restore = setLoading(event.submitter, "Входим...");
  const { username, password } = serializeForm(form);
  try {
    await loginWithPassword(username.trim(), password);
    form.reset();
    await bootstrap();
  } catch (error) {
    showToast(error.message);
  } finally {
    restore();
  }
});

document.addEventListener("click", async (event) => {
  const target = event.target;
  const demoLogin = target.dataset?.demoLogin;
  const openUserId = target.dataset?.openUser;
  const addFriendId = target.dataset?.addFriend;
  const removeFriendId = target.dataset?.removeFriend;
  const joinGroupId = target.dataset?.joinGroup;
  const leaveGroupId = target.dataset?.leaveGroup;
  const subscribeGroupId = target.dataset?.subscribeGroup;
  const unsubscribeGroupId = target.dataset?.unsubscribeGroup;
  const deleteWishId = target.dataset?.deleteWish;
  const readReminderId = target.dataset?.readReminder;

  try {
    if (demoLogin) {
      els.loginForm.username.value = demoLogin;
      els.loginForm.password.value = "password";
      await loginWithPassword(demoLogin, "password");
      await bootstrap();
    }
    if (openUserId) {
      state.selectedUserId = openUserId;
      setSection("people");
      renderUsers();
      await renderFriendCard();
    }
    if (addFriendId) {
      state.selectedUserId = addFriendId;
      await subscribe("USER", addFriendId);
      await Promise.all([loadCoreData(), refreshReminders()]);
      setSection("people");
      await renderFriendCard();
    }
    if (removeFriendId) {
      await unsubscribe("USER", removeFriendId);
      if (state.selectedUserId === removeFriendId) state.selectedUserId = "";
      await Promise.all([loadCoreData(), refreshReminders()]);
      setSection("people");
      showToast("Друг удален");
    }
    if (joinGroupId) {
      await api(`/api/groups/${joinGroupId}/members/me`, { method: "PUT" });
      showToast("Вы вступили в группу");
      await loadCoreData();
    }
    if (leaveGroupId) {
      await api(`/api/groups/${leaveGroupId}/members/me`, { method: "DELETE" });
      if (isGroupSubscribed(leaveGroupId)) {
        await unsubscribe("GROUP", leaveGroupId);
      }
      await loadCoreData();
      showToast("Вы вышли из группы");
    }
    if (subscribeGroupId) {
      await subscribeGroupAndMembers(subscribeGroupId);
      await Promise.all([loadCoreData(), refreshReminders()]);
    }
    if (unsubscribeGroupId) {
      await unsubscribe("GROUP", unsubscribeGroupId);
      await Promise.all([loadCoreData(), refreshReminders()]);
      showToast("Подписка на группу отключена");
    }
    if (deleteWishId) {
      await api(`/api/me/wishes/${deleteWishId}`, { method: "DELETE" });
      await loadCurrentUserWishes();
      if (state.selectedUserId === state.me?.id) await renderFriendCard();
      showToast("Желание удалено");
    }
    if (readReminderId) {
      await api(`/api/notifications/${readReminderId}/read`, { method: "PUT" });
      await refreshReminders();
    }
  } catch (error) {
    showToast(error.message);
  }
});

els.registerForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  const restore = setLoading(event.submitter, "Создаем...");
  const body = serializeForm(form);
  body.username = body.username.trim();
  try {
    await registerAccount(body);
    await loginWithPassword(body.username.trim(), body.password);
    form.reset();
    await bootstrap();
    showToast("Аккаунт создан");
  } catch (error) {
    showToast(error.message);
  } finally {
    restore();
  }
});

els.logoutButton.addEventListener("click", () => {
  clearSession();
  showAuth();
  showToast("Вы вышли из аккаунта");
});

els.profileForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  const restore = setLoading(event.submitter, "Сохраняем...");
  try {
    await api("/api/me/profile", {
      method: "POST",
      body: JSON.stringify(serializeForm(form)),
    });
    form.reset();
    await bootstrap();
    showToast("Профиль готов");
  } catch (error) {
    showToast(error.message);
  } finally {
    restore();
  }
});

els.refreshAllButton.addEventListener("click", async () => {
  const restore = setLoading(els.refreshAllButton, "Обновляем...");
  try {
    await loadCoreData();
    await Promise.allSettled([refreshReminders(), loadCurrentUserWishes()]);
    showToast("Данные обновлены");
  } catch (error) {
    showToast(error.message);
  } finally {
    restore();
  }
});

els.refreshRemindersButton.addEventListener("click", async () => {
  try {
    await refreshReminders();
    showToast("Напоминания обновлены");
  } catch (error) {
    showToast(error.message);
  }
});

els.peopleSearch.addEventListener("input", renderUsers);

$$("[data-section-link]").forEach((link) => {
  link.addEventListener("click", (event) => {
    event.preventDefault();
    setSection(link.dataset.sectionLink);
  });
});

els.subscribeFriendButton.addEventListener("click", async () => {
  try {
    if (isFriend(state.selectedUserId)) {
      await unsubscribe("USER", state.selectedUserId);
      state.selectedUserId = "";
      showToast("Друг удален");
    } else {
      await subscribe("USER", state.selectedUserId);
    }
    await Promise.all([loadCoreData(), refreshReminders()]);
    await renderFriendCard();
  } catch (error) {
    showToast(error.message);
  }
});

els.createGroupForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  try {
    await api("/api/groups", {
      method: "POST",
      body: JSON.stringify(serializeForm(form)),
    });
    form.reset();
    await loadCoreData();
    showToast("Группа создана");
  } catch (error) {
    showToast(error.message);
  }
});

els.createWishForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  try {
    const body = serializeForm(form);
    body.priority = Number(body.priority);
    await api("/api/me/wishes", {
      method: "POST",
      body: JSON.stringify(body),
    });
    form.reset();
    form.priority.value = 1;
    await loadCurrentUserWishes();
    if (state.selectedUserId === state.me?.id) await renderFriendCard();
    showToast("Желание опубликовано");
  } catch (error) {
    showToast(error.message);
  }
});

els.chatForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  const text = form.text.value.trim();
  if (!text) return;
  const payload = { text };
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
    form.reset();
  } catch (error) {
    showToast(error.message);
  }
});

window.addEventListener("beforeunload", closeChatSocket);

(async function init() {
  try {
    if (hasSession()) {
      await bootstrap();
    } else {
      showAuth();
    }
  } catch (error) {
    clearSession();
    showAuth();
    showToast(error.message);
  }
})();
