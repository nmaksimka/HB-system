const auth = {
  tokenUrl: "http://localhost:8080/realms/birthday-hub/protocol/openid-connect/token",
  clientId: "birthday-hub-ui",
};

const keys = {
  access: "birthdayHub.accessToken",
  refresh: "birthdayHub.refreshToken",
  expiresAt: "birthdayHub.expiresAt",
};

const MONTHS = ["январь", "февраль", "март", "апрель", "май", "июнь", "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь"];
const MONTHS_OF = ["января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря"];
const MONTHS_SHORT = ["янв", "фев", "мар", "апр", "мая", "июн", "июл", "авг", "сен", "окт", "ноя", "дек"];
const WEEKDAYS = ["воскресенье", "понедельник", "вторник", "среда", "четверг", "пятница", "суббота"];

const STATUS_TEXT = {
  400: "Проверьте введённые данные",
  401: "Сессия закончилась, войдите заново",
  403: "Нет доступа к этому действию",
  404: "Не нашли то, что вы ищете",
  409: "Данные уже изменились, обновите страницу",
  503: "Сервис временно недоступен, попробуйте через минуту",
};

const state = {
  me: null,
  users: new Map(),
  groups: [],
  subscriptions: [],
  chat: null,
  personToken: 0,
};

const $ = (selector, root = document) => root.querySelector(selector);
const screen = $("#screen");

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function safeLink(value) {
  try {
    const url = new URL(value);
    return url.protocol === "http:" || url.protocol === "https:" ? url.href : null;
  } catch {
    return null;
  }
}

let toastTimer;
function toast(message, isError = false) {
  const element = $("#toast");
  element.textContent = message;
  element.classList.toggle("error", isError);
  element.classList.add("visible");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => element.classList.remove("visible"), 3200);
}

function plural(n, one, few, many) {
  const mod10 = n % 10;
  const mod100 = n % 100;
  if (mod10 === 1 && mod100 !== 11) return one;
  if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return few;
  return many;
}

function parseDate(value) {
  const [year, month, day] = value.split("-").map(Number);
  return { year, month, day };
}

function isLeap(year) {
  return (year % 4 === 0 && year % 100 !== 0) || year % 400 === 0;
}

function todayUtc() {
  const now = new Date();
  return Date.UTC(now.getFullYear(), now.getMonth(), now.getDate());
}

function birthdayIn(year, birth) {
  const day = birth.month === 2 && birth.day === 29 && !isLeap(year) ? 28 : birth.day;
  return Date.UTC(year, birth.month - 1, day);
}

function nextBirthday(birthDate) {
  const birth = parseDate(birthDate);
  const today = todayUtc();
  const year = new Date(today).getUTCFullYear();
  let date = birthdayIn(year, birth);
  if (date < today) date = birthdayIn(year + 1, birth);
  const when = new Date(date);
  return {
    date: when,
    daysLeft: Math.round((date - today) / 86400000),
    turns: when.getUTCFullYear() - birth.year,
  };
}

function daysText(days) {
  if (days === 0) return "сегодня";
  if (days === 1) return "завтра";
  return `через ${days} ${plural(days, "день", "дня", "дней")}`;
}

function longDate(date) {
  return `${date.getUTCDate()} ${MONTHS_OF[date.getUTCMonth()]}`;
}

function money(value) {
  return `${Number(value).toLocaleString("ru-RU", { maximumFractionDigits: 2 })} ₽`;
}

function sheet(date) {
  return `
    <div class="sheet" aria-hidden="true">
      <span class="sheet-month">${MONTHS[date.getUTCMonth()]}</span>
      <span class="sheet-day">${date.getUTCDate()}</span>
      <span class="sheet-weekday">${WEEKDAYS[date.getUTCDay()]}</span>
    </div>`;
}

function dateChip(date) {
  return `<span class="date-chip" aria-hidden="true"><b>${date.getUTCDate()}</b><small>${MONTHS_SHORT[date.getUTCMonth()]}</small></span>`;
}

function saveTokens(tokens) {
  localStorage.setItem(keys.access, tokens.access_token);
  localStorage.setItem(keys.refresh, tokens.refresh_token ?? "");
  localStorage.setItem(keys.expiresAt, String(Date.now() + Number(tokens.expires_in ?? 300) * 1000));
}

function clearTokens() {
  Object.values(keys).forEach((key) => localStorage.removeItem(key));
}

async function requestToken(form) {
  const response = await fetch(auth.tokenUrl, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ client_id: auth.clientId, ...form }),
  });
  if (!response.ok) {
    const error = new Error(response.status === 401 ? "Неверный логин или пароль" : "Не удалось войти");
    error.status = response.status;
    throw error;
  }
  saveTokens(await response.json());
}

async function freshToken(force = false) {
  const expiresAt = Number(localStorage.getItem(keys.expiresAt) ?? 0);
  const refreshToken = localStorage.getItem(keys.refresh);
  if (refreshToken && (force || Date.now() > expiresAt - 30000)) {
    await requestToken({ grant_type: "refresh_token", refresh_token: refreshToken });
  }
  return localStorage.getItem(keys.access);
}

async function api(path, { method = "GET", body, errors = {} } = {}, retried = false) {
  let token;
  try {
    token = await freshToken();
  } catch {
    signOut();
    throw new Error(STATUS_TEXT[401]);
  }
  const response = await fetch(path, {
    method,
    headers: {
      Authorization: `Bearer ${token}`,
      ...(body === undefined ? {} : { "Content-Type": "application/json" }),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (response.status === 401 && !retried) {
    try {
      await freshToken(true);
    } catch {
      signOut();
      throw new Error(STATUS_TEXT[401]);
    }
    return api(path, { method, body, errors }, true);
  }
  if (!response.ok) {
    const error = new Error(errors[response.status] ?? STATUS_TEXT[response.status] ?? "Что-то пошло не так");
    error.status = response.status;
    throw error;
  }
  return response.status === 204 ? null : response.json();
}

async function busy(button, action) {
  if (button) button.disabled = true;
  try {
    return await action();
  } catch (error) {
    toast(error.message, true);
    return undefined;
  } finally {
    if (button) button.disabled = false;
  }
}

function showOnly(id) {
  ["authView", "profileView", "appView"].forEach((view) => $(`#${view}`).classList.toggle("hidden", view !== id));
}

function signOut() {
  closeChat();
  clearTokens();
  state.me = null;
  showAuth();
}

function showAuth() {
  const today = new Date(todayUtc());
  $("#authMonth").textContent = MONTHS[today.getUTCMonth()];
  $("#authDay").textContent = today.getUTCDate();
  $("#authWeekday").textContent = WEEKDAYS[today.getUTCDay()];
  showOnly("authView");
}

function setAuthMode(mode) {
  document.querySelectorAll("[data-auth-mode]").forEach((tab) => tab.classList.toggle("active", tab.dataset.authMode === mode));
  $("#loginForm").classList.toggle("hidden", mode !== "login");
  $("#registerForm").classList.toggle("hidden", mode !== "register");
}

async function start() {
  if (!localStorage.getItem(keys.access)) {
    showAuth();
    return;
  }
  try {
    state.me = await api("/api/me");
  } catch (error) {
    if (error.status === 404) {
      showOnly("profileView");
      return;
    }
    signOut();
    return;
  }
  $("#accountName").textContent = state.me.name;
  showOnly("appView");
  await loadSocial();
  route();
}

async function loadSocial() {
  const [users, groups, subscriptions] = await Promise.all([
    api("/api/users"),
    api("/api/groups"),
    api("/api/subscriptions"),
  ]);
  state.users = new Map(users.map((user) => [user.id, user]));
  state.groups = groups;
  state.subscriptions = subscriptions;
}

function isFriend(userId) {
  return state.subscriptions.some((s) => s.targetType === "USER" && s.targetId === userId);
}

function watchesGroup(groupId) {
  return state.subscriptions.some((s) => s.targetType === "GROUP" && s.targetId === groupId);
}

function nameOf(userId) {
  if (userId === state.me?.id) return "вы";
  return state.users.get(userId)?.name ?? "кто-то";
}

function route() {
  closeChat();
  const [tab, id] = (location.hash.slice(1) || "upcoming").split("/");
  document.querySelectorAll("[data-tab]").forEach((link) => {
    link.classList.toggle("active", link.dataset.tab === tab || (tab === "person" && link.dataset.tab === "people"));
  });
  const views = { upcoming: renderUpcoming, people: renderPeople, person: () => renderPerson(id), groups: renderGroups, wishes: renderWishes };
  (views[tab] ?? renderUpcoming)().catch((error) => toast(error.message, true));
  window.scrollTo(0, 0);
}

async function renderUpcoming() {
  const [upcoming, reminders] = await Promise.all([
    api("/api/me/upcoming"),
    api("/api/notifications/refresh", { method: "POST" }),
  ]);
  const unread = reminders.filter((r) => !r.readAt).length;
  const [next, ...rest] = upcoming;

  const hero = next ? `
    <section class="next">
      ${sheet(new Date(`${next.nextBirthday}T00:00:00Z`))}
      <div>
        <p class="muted">Следующий день рождения</p>
        <p class="next-name">${escapeHtml(next.name)}</p>
        <p class="next-when"><strong>${daysText(next.daysLeft)}</strong>, исполнится ${nextBirthday(next.birthDate).turns}</p>
        <a class="button primary" href="#person/${next.id}">Подготовить подарок</a>
      </div>
    </section>` : `
    <p class="empty">Здесь появятся ближайшие дни рождения. Добавьте друзей во вкладке <a href="#people">«Люди»</a> или вступите в <a href="#groups">группу</a>.</p>`;

  const list = rest.length ? `<ul class="list">${rest.map((u) => `
    <li>
      ${dateChip(new Date(`${u.nextBirthday}T00:00:00Z`))}
      <button type="button" class="person grow" data-open="${u.id}">
        <span class="title">${escapeHtml(u.name)}</span>
        <span class="meta">исполнится ${nextBirthday(u.birthDate).turns}</span>
      </button>
      <span class="days ${u.daysLeft <= 7 ? "soon" : ""}">${daysText(u.daysLeft)}</span>
    </li>`).join("")}</ul>` : "";

  const reminderList = reminders.length ? `<ul class="list">${reminders.map((r) => `
    <li class="reminder ${r.readAt ? "" : "unread"}">
      <button type="button" class="person grow" data-open="${r.birthdayUserId}">
        <span class="title">${escapeHtml(r.birthdayUserName)}</span>
        <span class="meta">${longDate(new Date(`${r.birthdayDate}T00:00:00Z`))}, ${reminderText(r.thresholdDays)}</span>
      </button>
      ${r.readAt ? "" : `<button type="button" class="button quiet" data-read="${r.id}">Прочитано</button>`}
    </li>`).join("")}</ul>` : `<p class="empty">Напоминаний пока нет. Они появятся за месяц, неделю и день до праздника.</p>`;

  screen.innerHTML = `
    <div class="split">
      <div>
        ${hero}
        ${list}
      </div>
      <aside>
        <div class="panel-title">
          <h2>Напоминания${unread ? ` (${unread})` : ""}</h2>
          ${unread ? `<button type="button" class="link" data-read-all>Прочитать все</button>` : ""}
        </div>
        ${reminderList}
      </aside>
    </div>`;
}

function reminderText(threshold) {
  if (threshold === 0) return "сегодня";
  if (threshold === 1) return "уже завтра";
  if (threshold === 7) return "осталось меньше недели";
  return "осталось меньше месяца";
}

async function renderPeople() {
  await loadSocial();
  const people = [...state.users.values()]
    .filter((user) => user.id !== state.me.id)
    .map((user) => ({ ...user, next: nextBirthday(user.birthDate) }))
    .sort((a, b) => a.next.daysLeft - b.next.daysLeft);

  screen.innerHTML = `
    <div class="screen-head">
      <div>
        <h2>Люди</h2>
        <p class="muted">Добавьте человека в друзья, чтобы получать напоминания и попасть в секретный чат про его подарок.</p>
      </div>
    </div>
    <input class="search" type="search" placeholder="Найти по имени" data-search aria-label="Найти по имени">
    ${people.length ? `<ul class="list" data-people>${people.map((user) => `
      <li data-name="${escapeHtml(user.name.toLowerCase())}">
        ${dateChip(user.next.date)}
        <button type="button" class="person grow" data-open="${user.id}">
          <span class="title">${escapeHtml(user.name)}</span>
          <span class="meta">${daysText(user.next.daysLeft)}</span>
        </button>
        ${isFriend(user.id) ? `<span class="badge friend">друг</span>` : ""}
        ${friendButton(user.id)}
      </li>`).join("")}</ul>` : `<p class="empty">Пока здесь только вы. Позовите друзей зарегистрироваться.</p>`}`;
}

function friendButton(userId) {
  return isFriend(userId)
    ? `<button type="button" class="button quiet" data-unfriend="${userId}">Убрать из друзей</button>`
    : `<button type="button" class="button" data-friend="${userId}">Добавить в друзья</button>`;
}

async function renderPerson(userId) {
  if (!userId) return renderPeople();
  if (userId === state.me.id) {
    location.hash = "#wishes";
    return;
  }
  const token = ++state.personToken;
  const [card, wishes, fundraisers] = await Promise.all([
    api(`/api/users/${userId}/card`),
    api(`/api/users/${userId}/wishes`),
    api(`/api/users/${userId}/fundraisers`),
  ]);
  const details = await Promise.all(fundraisers.map((f) => api(`/api/fundraisers/${f.id}`)));
  if (token !== state.personToken) return;

  const next = nextBirthday(card.birthDate);
  const activeByWish = new Map(details.filter((f) => f.status === "ACTIVE").map((f) => [f.wishId, f]));
  const wishTitles = new Map(wishes.map((w) => [w.id, w.title]));

  screen.innerHTML = `
    <div class="split">
      <div>
        <section class="person-head">
          ${sheet(next.date)}
          <div class="grow">
            <h1>${escapeHtml(card.name)}</h1>
            <p class="next-when"><strong>${daysText(next.daysLeft)}</strong>, исполнится ${next.turns}</p>
            ${card.groups.length ? `<div class="chips">${card.groups.map((g) => `<span class="badge">${escapeHtml(g)}</span>`).join("")}</div>` : ""}
          </div>
          ${friendButton(card.id)}
        </section>

        <section class="section">
          <div class="panel-title"><h2>Список желаний</h2></div>
          ${wishes.length ? `<ul class="list">${wishes.map((wish) => wishRow(wish, card.id, activeByWish.get(wish.id))).join("")}</ul>`
            : `<p class="empty">${escapeHtml(card.name)} пока ничего не добавил в список. Обсудите идеи в чате.</p>`}
        </section>

        <section class="section">
          <div class="panel-title"><h2>Сборы на подарок</h2></div>
          ${details.length ? details.map((f) => fundraiserBlock(f, wishTitles)).join("")
            : `<p class="empty">Сборов пока нет. Нажмите «Скинуться» у желания, чтобы собрать деньги вместе.</p>`}
        </section>
      </div>
      <aside id="chatSlot"></aside>
    </div>`;

  openChat(card);
}

function wishRow(wish, ownerId, fundraiser) {
  const link = safeLink(wish.link);
  let status = "";
  let action = `<button type="button" class="button" data-reserve="${wish.id}" data-owner="${ownerId}">Забронировать</button>`;
  if (wish.reservedByMe) {
    status = `<span class="badge reserved">вы дарите это</span>`;
    action = `<button type="button" class="button quiet" data-unreserve="${wish.id}" data-owner="${ownerId}">Снять бронь</button>`;
  } else if (wish.reserved) {
    status = `<span class="badge">уже забронировано</span>`;
    action = "";
  }
  const chip = fundraiser
    ? `<span class="badge reserved">идёт сбор</span>`
    : `<button type="button" class="button quiet" data-fund="${wish.id}" data-title="${escapeHtml(wish.title)}">Скинуться</button>`;
  return `
    <li>
      <div class="grow">
        <div class="title">${escapeHtml(wish.title)}</div>
        ${wish.description ? `<div class="meta">${escapeHtml(wish.description)}</div>` : ""}
        ${link ? `<a class="wish-link" href="${escapeHtml(link)}" target="_blank" rel="noopener noreferrer">Посмотреть в магазине</a>` : ""}
      </div>
      ${status}
      ${chip}
      ${action}
    </li>`;
}

function fundraiserBlock(fundraiser, wishTitles) {
  const target = Number(fundraiser.targetAmount);
  const collected = Number(fundraiser.collectedAmount);
  const percent = Math.min(100, Math.round((collected / target) * 100));
  const statusText = { ACTIVE: "идёт сбор", COMPLETED: "собрали", CANCELLED: "отменён" }[fundraiser.status];
  const totals = new Map();
  fundraiser.contributions.forEach((c) => totals.set(c.payerId, (totals.get(c.payerId) ?? 0) + Number(c.amount)));
  const people = [...totals.entries()].map(([id, sum]) => `${escapeHtml(nameOf(id))} — ${money(sum)}`).join(", ");
  const left = Math.max(0, target - collected);
  const isOwner = fundraiser.ownerId === state.me.id;

  return `
    <div class="fund">
      <div class="fund-head">
        <div>
          <div class="title">${escapeHtml(fundraiser.title)}</div>
          <div class="meta">на «${escapeHtml(wishTitles.get(fundraiser.wishId) ?? "подарок")}», собирает ${escapeHtml(nameOf(fundraiser.ownerId))}</div>
        </div>
        <span class="badge ${fundraiser.status === "ACTIVE" ? "reserved" : ""}">${statusText}</span>
      </div>
      <div class="progress" role="progressbar" aria-valuemin="0" aria-valuemax="100" aria-valuenow="${percent}"><span style="width:${percent}%"></span></div>
      <div class="fund-numbers"><span><b>${money(collected)}</b> из ${money(target)}</span><span>${percent}%</span></div>
      ${people ? `<p class="contributors">Скинулись: ${people}</p>` : ""}
      ${fundraiser.status === "ACTIVE" ? `
        <form class="pay" data-pay="${fundraiser.id}" data-key="${crypto.randomUUID()}">
          <input name="amount" type="number" min="1" max="${left}" step="1" value="${Math.min(500, left)}" aria-label="Сумма взноса" required>
          <button type="submit" class="button money">Внести</button>
          ${isOwner ? `<button type="button" class="button quiet" data-cancel-fund="${fundraiser.id}">Отменить сбор</button>` : ""}
        </form>` : ""}
    </div>`;
}

async function renderGroups() {
  await loadSocial();
  const groups = state.groups.map((group) => {
    const names = group.memberIds.filter((id) => id !== state.me.id).map(nameOf);
    const member = group.memberIds.includes(state.me.id);
    return `
      <li>
        <div class="grow">
          <div class="title">${escapeHtml(group.name)}</div>
          <div class="meta">${group.memberIds.length} ${plural(group.memberIds.length, "участник", "участника", "участников")}${names.length ? `: ${escapeHtml(names.slice(0, 4).join(", "))}${names.length > 4 ? " и другие" : ""}` : ""}</div>
        </div>
        ${member ? `<span class="badge friend">вы в группе</span>` : ""}
        ${watchesGroup(group.id)
          ? `<button type="button" class="button quiet" data-unwatch="${group.id}">Не напоминать</button>`
          : `<button type="button" class="button" data-watch="${group.id}">Напоминать о всех</button>`}
        ${member
          ? `<button type="button" class="button quiet" data-leave="${group.id}">Выйти</button>`
          : `<button type="button" class="button" data-join="${group.id}">Вступить</button>`}
      </li>`;
  }).join("");

  screen.innerHTML = `
    <div class="split">
      <div>
        <div class="screen-head">
          <div>
            <h2>Группы</h2>
            <p class="muted">Участники одной группы видят секретные чаты друг о друге и могут напоминать себе обо всех сразу.</p>
          </div>
        </div>
        ${groups ? `<ul class="list">${groups}</ul>` : `<p class="empty">Групп пока нет. Создайте первую для своей компании.</p>`}
      </div>
      <aside>
        <form class="add-wish form" data-create-group>
          <h3>Новая группа</h3>
          <label>Название<input name="name" maxlength="160" placeholder="Например, 301 группа" required></label>
          <button type="submit" class="button primary">Создать группу</button>
        </form>
      </aside>
    </div>`;
}

async function renderWishes() {
  const wishes = await api("/api/me/wishes");
  screen.innerHTML = `
    <div class="split">
      <div>
        <div class="screen-head">
          <div>
            <h2>Мой список желаний</h2>
            <p class="muted">Друзья видят этот список и бронируют подарки. Кто и что забронировал, вам не покажем.</p>
          </div>
        </div>
        ${wishes.length ? `<ul class="list">${wishes.map((wish) => {
          const link = safeLink(wish.link);
          return `
            <li>
              <span class="badge" title="Приоритет">${wish.priority}</span>
              <div class="grow">
                <div class="title">${escapeHtml(wish.title)}</div>
                ${wish.description ? `<div class="meta">${escapeHtml(wish.description)}</div>` : ""}
                ${link ? `<a class="wish-link" href="${escapeHtml(link)}" target="_blank" rel="noopener noreferrer">Ссылка</a>` : ""}
              </div>
              <button type="button" class="button quiet" data-delete-wish="${wish.id}">Удалить</button>
            </li>`;
        }).join("")}</ul>` : `<p class="empty">Список пуст. Добавьте пару идей, чтобы друзьям было проще выбрать подарок.</p>`}
      </div>
      <aside>
        <form class="add-wish form" data-create-wish>
          <h3>Добавить желание</h3>
          <label>Что подарить<input name="title" maxlength="160" required></label>
          <label>Подробности<textarea name="description" maxlength="1000" placeholder="Размер, цвет, модель"></textarea></label>
          <div class="two">
            <label>Ссылка<input name="link" type="url" maxlength="500" placeholder="https://"></label>
            <label>Приоритет<input name="priority" type="number" min="1" max="10" value="5" required></label>
          </div>
          <button type="submit" class="button primary">Добавить</button>
        </form>
      </aside>
    </div>`;
}

function openChat(card) {
  const slot = $("#chatSlot");
  slot.innerHTML = `
    <section class="chat">
      <div class="chat-head">
        <h3>Секретный чат</h3>
        <p class="muted" data-chat-status>Загружаем сообщения…</p>
      </div>
      <div class="messages" data-messages></div>
      <form class="chat-form" data-chat-form="${card.id}">
        <input name="text" maxlength="2000" placeholder="Идея подарка или план" autocomplete="off" aria-label="Сообщение" required>
        <button type="submit" class="button primary">Отправить</button>
      </form>
    </section>`;

  const chat = { subjectId: card.id, seen: new Set(), socket: null, closed: false };
  state.chat = chat;
  const status = $("[data-chat-status]", slot);

  api(`/api/chat/users/${card.id}/messages`, { errors: { 403: "locked" } })
    .then((messages) => {
      if (chat.closed) return;
      messages.forEach(appendMessage);
      status.textContent = `${card.name} этот чат не видит`;
      connectChat(chat, status);
    })
    .catch((error) => {
      if (chat.closed) return;
      if (error.status === 403) {
        slot.querySelector(".messages").outerHTML = `<p class="chat-locked">Чат открыт друзьям и участникам общих групп. Добавьте ${escapeHtml(card.name)} в друзья, чтобы обсуждать подарок.</p>`;
        slot.querySelector(".chat-form").remove();
        status.textContent = "Нет доступа";
      } else {
        status.textContent = error.message;
      }
    });
}

function appendMessage(message) {
  const chat = state.chat;
  const list = $("[data-messages]");
  if (!chat || !list || chat.seen.has(message.id)) return;
  chat.seen.add(message.id);
  const mine = message.authorId === state.me.id;
  const time = new Date(message.createdAt).toLocaleString("ru-RU", { day: "numeric", month: "short", hour: "2-digit", minute: "2-digit" });
  list.insertAdjacentHTML("beforeend", `
    <div class="message ${mine ? "mine" : ""}">
      <div class="who">${mine ? "вы" : escapeHtml(nameOf(message.authorId))}, ${time}</div>
      <div class="text">${escapeHtml(message.text)}</div>
    </div>`);
  list.scrollTop = list.scrollHeight;
}

async function connectChat(chat, status) {
  const token = await freshToken().catch(() => null);
  if (!token || chat.closed) return;
  const protocol = location.protocol === "https:" ? "wss" : "ws";
  const socket = new WebSocket(`${protocol}://${location.host}/ws`);
  chat.socket = socket;

  socket.addEventListener("open", () => {
    socket.send(`CONNECT\naccept-version:1.2\nheart-beat:0,0\nAuthorization:Bearer ${token}\n\n\0`);
  });
  socket.addEventListener("message", (event) => {
    String(event.data).split("\0").map((frame) => frame.replace(/^\n+/, "")).filter(Boolean).forEach((frame) => {
      const separator = frame.indexOf("\n\n");
      const command = frame.slice(0, frame.indexOf("\n"));
      const body = separator >= 0 ? frame.slice(separator + 2) : "";
      if (command === "CONNECTED") {
        socket.send(`SUBSCRIBE\nid:chat\ndestination:/topic/chat/${chat.subjectId}\n\n\0`);
        status.classList.add("live");
      } else if (command === "MESSAGE") {
        appendMessage(JSON.parse(body));
      } else if (command === "ERROR") {
        status.classList.remove("live");
      }
    });
  });
  socket.addEventListener("close", () => status.classList.remove("live"));
}

function closeChat() {
  if (!state.chat) return;
  state.chat.closed = true;
  state.chat.socket?.close();
  state.chat = null;
}

function formData(form) {
  return Object.fromEntries(new FormData(form).entries());
}

async function changeAndRerender(button, action, message) {
  const done = await busy(button, async () => {
    await action();
    return true;
  });
  if (!done) return;
  if (message) toast(message);
  await loadSocial();
  route();
}

screen.addEventListener("click", async (event) => {
  const target = event.target.closest("button, a");
  if (!target) return;
  const data = target.dataset;

  if (data.open) {
    location.hash = `#person/${data.open}`;
  } else if (data.friend) {
    await changeAndRerender(target, () => api(`/api/subscriptions/users/${data.friend}`, { method: "POST" }), "Добавили в друзья");
  } else if (data.unfriend) {
    await changeAndRerender(target, () => api(`/api/subscriptions/users/${data.unfriend}`, { method: "DELETE" }));
  } else if (data.watch) {
    await changeAndRerender(target, () => api(`/api/subscriptions/groups/${data.watch}`, { method: "POST" }), "Будем напоминать обо всех в группе");
  } else if (data.unwatch) {
    await changeAndRerender(target, () => api(`/api/subscriptions/groups/${data.unwatch}`, { method: "DELETE" }));
  } else if (data.join) {
    await changeAndRerender(target, () => api(`/api/groups/${data.join}/members/me`, { method: "PUT" }), "Вы в группе");
  } else if (data.leave) {
    await changeAndRerender(target, () => api(`/api/groups/${data.leave}/members/me`, { method: "DELETE" }));
  } else if (data.reserve) {
    await changeAndRerender(target, () => api(`/api/users/${data.owner}/wishes/${data.reserve}/reservation`, {
      method: "PUT",
      errors: { 409: "Этот подарок уже забронировал кто-то другой" },
    }), "Забронировано, теперь это ваш подарок");
  } else if (data.unreserve) {
    await changeAndRerender(target, () => api(`/api/users/${data.owner}/wishes/${data.unreserve}/reservation`, { method: "DELETE" }));
  } else if (data.fund) {
    openFundraiserDialog(data.fund, data.title);
  } else if (data.cancelFund) {
    if (!confirm("Отменить сбор? Новые взносы перестанут приниматься.")) return;
    await changeAndRerender(target, () => api(`/api/fundraisers/${data.cancelFund}/cancel`, { method: "POST" }), "Сбор отменён");
  } else if (data.deleteWish) {
    await changeAndRerender(target, () => api(`/api/me/wishes/${data.deleteWish}`, { method: "DELETE" }));
  } else if (data.read) {
    await changeAndRerender(target, () => api(`/api/notifications/${data.read}/read`, { method: "PUT" }));
  } else if (target.hasAttribute("data-read-all")) {
    await changeAndRerender(target, () => api("/api/notifications/read-all", { method: "PUT" }));
  }
});

screen.addEventListener("input", (event) => {
  if (!event.target.matches("[data-search]")) return;
  const query = event.target.value.trim().toLowerCase();
  document.querySelectorAll("[data-people] > li").forEach((row) => {
    row.classList.toggle("hidden", !row.dataset.name.includes(query));
  });
});

screen.addEventListener("submit", async (event) => {
  const form = event.target;
  event.preventDefault();
  const button = form.querySelector("button[type=submit]");
  const values = formData(form);

  if (form.dataset.chatForm) {
    const message = await busy(button, () => api(`/api/chat/users/${form.dataset.chatForm}/messages`, {
      method: "POST",
      body: { text: values.text },
    }));
    if (message) {
      appendMessage(message);
      form.reset();
      form.elements.text.focus();
    }
  } else if (form.dataset.pay) {
    const payment = await busy(button, () => api(`/api/bank/collections/${form.dataset.pay}/payments`, {
      method: "POST",
      body: { amount: Number(values.amount), idempotencyKey: form.dataset.key },
      errors: { 400: "Сумма больше, чем осталось собрать", 409: "Сбор уже закрыт" },
    }));
    if (payment) {
      toast(`Внесли ${money(payment.amount)}. Сумма обновится через пару секунд`);
      setTimeout(route, 1500);
    }
  } else if (form.hasAttribute("data-create-group")) {
    await changeAndRerender(button, () => api("/api/groups", {
      method: "POST",
      body: { name: values.name },
      errors: { 409: "Группа с таким названием уже есть" },
    }), "Группа создана");
  } else if (form.hasAttribute("data-create-wish")) {
    await changeAndRerender(button, () => api("/api/me/wishes", {
      method: "POST",
      body: {
        title: values.title,
        description: values.description || null,
        link: values.link || null,
        priority: Number(values.priority),
      },
      errors: { 400: "Проверьте ссылку: она должна начинаться с http:// или https://" },
    }), "Добавили в список");
  }
});

function openFundraiserDialog(wishId, wishTitle) {
  const dialog = $("#fundraiserDialog");
  const form = $("#fundraiserForm");
  form.reset();
  form.dataset.wish = wishId;
  form.elements.title.value = `Скидываемся на ${wishTitle}`;
  $("#fundraiserWish").textContent = `Подарок: ${wishTitle}`;
  dialog.showModal();
}

$("#fundraiserForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = event.target;
  const values = formData(form);
  const created = await busy(form.querySelector("button[type=submit]"), () => api("/api/fundraisers", {
    method: "POST",
    body: { wishId: form.dataset.wish, title: values.title, targetAmount: Number(values.targetAmount) },
    errors: { 409: "На этот подарок уже идёт сбор" },
  }));
  if (created) {
    $("#fundraiserDialog").close();
    toast("Сбор открыт, зовите друзей");
    route();
  }
});

document.querySelectorAll("[data-close-dialog]").forEach((button) => {
  button.addEventListener("click", () => button.closest("dialog").close());
});

document.querySelectorAll("[data-auth-mode]").forEach((tab) => {
  tab.addEventListener("click", () => setAuthMode(tab.dataset.authMode));
});

document.querySelectorAll("[data-demo]").forEach((button) => {
  button.addEventListener("click", () => {
    $("#loginForm").username.value = button.dataset.demo;
    $("#loginForm").password.value = "password";
  });
});

$("#loginForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const values = formData(event.target);
  const ok = await busy(event.target.querySelector("button[type=submit]"), async () => {
    await requestToken({ grant_type: "password", username: values.username, password: values.password, scope: "openid" });
    return true;
  });
  if (ok) start();
});

$("#registerForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const values = formData(event.target);
  const ok = await busy(event.target.querySelector("button[type=submit]"), async () => {
    const response = await fetch("/api/auth/register", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(values),
    });
    if (!response.ok) {
      throw new Error(response.status === 409 ? "Этот логин уже занят" : STATUS_TEXT[response.status] ?? "Не удалось зарегистрироваться");
    }
    await requestToken({ grant_type: "password", username: values.username, password: values.password, scope: "openid" });
    return true;
  });
  if (ok) start();
});

$("#profileForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const values = formData(event.target);
  const ok = await busy(event.target.querySelector("button[type=submit]"), async () => {
    await api("/api/me/profile", { method: "POST", body: values });
    return true;
  });
  if (ok) start();
});

$("#logoutButton").addEventListener("click", signOut);
window.addEventListener("hashchange", () => {
  if (state.me) route();
});

start();
