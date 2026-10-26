
const API_BASE_URL = "/api";

const foodList = document.getElementById("food-list");
const recommendationList = document.getElementById("recommendation-list");
const menuStatus = document.getElementById("menu-status");
const searchInput = document.getElementById("food-search");
const categoryList = document.getElementById("category-list");
const emptyState = document.getElementById("empty-state");

const cartCount = document.getElementById("cart-count");
const cartCountHeading = document.getElementById("cart-count-heading");
const cartItems = document.getElementById("cart-items");
const cartTotal = document.getElementById("cart-total");
const checkoutButton = document.getElementById("checkout-button");

const checkoutSection = document.getElementById("checkout-section");
const checkoutForm = document.getElementById("checkout-form");
const checkoutMessage = document.getElementById("checkout-message");
const placeOrderButton = document.getElementById("place-order-button");
const confirmationSection = document.getElementById("confirmation-section");
const toastContainer = document.getElementById("toast-container");
const trackingForm = document.getElementById("tracking-form");
const trackingMessage = document.getElementById("tracking-message");
const trackingResult = document.getElementById("tracking-result");
const trackingTimeline = document.getElementById("tracking-timeline");
const accountLink = document.getElementById("account-link");
const signedOutView = document.getElementById("signed-out-view");
const signedInView = document.getElementById("signed-in-view");
const adminDashboard = document.getElementById("admin-dashboard");
const notificationButton = document.getElementById("notification-button");
const notificationList = document.getElementById("notification-list");

let foods = [];
let popularFoods = [];
let cart = [];
let activeCategory = "All";
let searchTerm = "";
let isSubmitting = false;
let trackedOrderId = null;
let lastTrackedStatus = null;
let trackingTimer = null;
let currentUser = null;
let notificationTimer = null;
let seenNotificationIds = new Set();
let authToken = "";

try {
    authToken = sessionStorage.getItem("campusBiteToken") || "";
} catch (error) {
    console.error("Could not restore sign-in state:", error);
}

document.getElementById("current-year").textContent =
    new Date().getFullYear();

// Restore cart safely.
try {
    const savedCart = JSON.parse(
        localStorage.getItem("campusBiteCart") || "[]"
    );

    if (Array.isArray(savedCart)) {
        cart = savedCart
            .filter(item =>
                Number.isInteger(Number(item.id)) &&
                Number.isInteger(Number(item.quantity)) &&
                Number(item.quantity) > 0 &&
                Number.isFinite(Number(item.price)) &&
                Number(item.price) >= 0
            )
            .map(item => ({
                id: Number(item.id),
                name: String(item.name || "Food item"),
                price: Number(item.price),
                quantity: Number(item.quantity)
            }));
    }
} catch {
    cart = [];
}

function saveCart() {
    try {
        localStorage.setItem("campusBiteCart", JSON.stringify(cart));
    } catch (error) {
        console.error("Could not save cart:", error);
    }
}

function money(amount) {
    return "₹" + Number(amount).toFixed(2);
}

function showToast(message, type = "success") {
    const toast = document.createElement("div");
    toast.className = `toast${type === "error" ? " toast-error" : ""}`;
    toast.setAttribute("role", "status");

    const icon = document.createElement("span");
    icon.className = "toast-icon";
    icon.textContent = type === "error" ? "!" : "✓";

    const text = document.createElement("span");
    text.textContent = message;

    toast.append(icon, text);
    toastContainer.appendChild(toast);

    const removeToast = () => {
        toast.classList.add("toast-out");
        toast.addEventListener("animationend", () => toast.remove(), {
            once: true
        });
        setTimeout(() => toast.remove(), 350);
    };

    setTimeout(removeToast, 2800);
}

async function apiFetch(path, options = {}) {
    const headers = new Headers(options.headers || {});
    if (authToken) headers.set("Authorization", `Bearer ${authToken}`);
    if (options.body && !headers.has("Content-Type")) {
        headers.set("Content-Type", "application/json");
    }
    const response = await fetch(`${API_BASE_URL}${path}`, { ...options, headers });
    if (response.status === 401 && authToken &&
        !path.startsWith("/auth/login") &&
        !path.startsWith("/auth/register")) {
        signOut();
    }
    return response;
}

async function responseError(response, fallback) {
    const text = await response.text();
    try {
        const payload = text ? JSON.parse(text) : {};
        if (payload.message) return payload.message;
        if (payload.error) return payload.error;
    } catch {
        if (text && text.length < 250) return text;
    }
    return `${fallback} (HTTP ${response.status}).`;
}

function showAccountMessage(elementId, message) {
    document.getElementById(elementId).textContent = message || "";
}

function updateAccountViews() {
    const signedIn = Boolean(currentUser);
    signedOutView.hidden = signedIn;
    signedInView.hidden = !signedIn;
    adminDashboard.hidden = !signedIn || currentUser.role !== "ADMIN";
    notificationButton.hidden = !signedIn;
    accountLink.textContent = signedIn
        ? (currentUser.name.split(" ")[0] || "Account")
        : "Sign in";

    if (signedIn) {
        document.getElementById("profile-heading").textContent =
            `Hello, ${currentUser.name}`;
        document.getElementById("profile-role").textContent =
            currentUser.role === "ADMIN" ? "STAFF" : "✓";
        document.getElementById("profile-name").value = currentUser.name;
        document.getElementById("profile-phone").value = currentUser.phone;
        if (currentUser.role === "ADMIN") {
            loadAdminData();
        }
        loadMyOrders();
        loadNotifications();
        if (!notificationTimer) {
            notificationTimer = setInterval(loadNotifications, 30000);
        }
    } else {
        clearInterval(notificationTimer);
        notificationTimer = null;
        notificationList.hidden = true;
    }
}

async function acceptAuthentication(response) {
    const data = await response.json();
    authToken = data.token;
    currentUser = data.user;
    try {
        sessionStorage.setItem("campusBiteToken", authToken);
    } catch (error) {
        console.error("Could not store sign-in token for this tab:", error);
    }
    updateAccountViews();
    showToast(`Signed in as ${currentUser.name}.`);
}

async function restoreAuthentication() {
    if (!authToken) return;
    try {
        const response = await apiFetch("/auth/me");
        if (!response.ok) {
            authToken = "";
            sessionStorage.removeItem("campusBiteToken");
            return;
        }
        currentUser = await response.json();
        updateAccountViews();
    } catch (error) {
        console.error("Could not restore account profile:", error);
    }
}

async function loadNotifications() {
    if (!currentUser) return;
    try {
        const response = await apiFetch("/notifications");
        if (!response.ok) throw new Error(await responseError(response, "Could not load notifications"));
        const notifications = await response.json();
        if (!Array.isArray(notifications)) {
            throw new Error("Unexpected notification response");
        }

        const unread = notifications.filter(notification => !notification.readAt);
        document.getElementById("notification-count").textContent = unread.length;
        notificationList.replaceChildren();
        if (!notifications.length) {
            const empty = document.createElement("p");
            empty.className = "cart-empty";
            empty.textContent = "No order updates yet.";
            notificationList.appendChild(empty);
        }
        notifications.forEach(notification => {
            if (!seenNotificationIds.has(notification.id) && seenNotificationIds.size) {
                showToast(notification.message);
            }
            seenNotificationIds.add(notification.id);

            const button = document.createElement("button");
            button.type = "button";
            button.className = `notification-item${notification.readAt ? "" : " unread"}`;
            button.textContent = notification.message;
            button.addEventListener("click", async () => {
                if (!notification.readAt) {
                    const updateResponse = await apiFetch(
                        `/notifications/${notification.id}/read`, { method: "PATCH" });
                    if (!updateResponse.ok) {
                        showToast("Could not mark the notification as read.", "error");
                        return;
                    }
                }
                notificationList.hidden = true;
                startTracking(notification.orderId);
                document.getElementById("order-tracking").scrollIntoView({
                    behavior: "smooth", block: "start"
                });
            });
            notificationList.appendChild(button);
        });
    } catch (error) {
        console.error("Notification refresh failed:", error);
    }
}

async function loadMyOrders() {
    if (!currentUser) return;
    try {
        const response = await apiFetch("/orders");
        if (!response.ok) throw new Error(await responseError(response, "Could not load orders"));
        const orders = await response.json();
        const list = document.getElementById("my-order-list");
        list.replaceChildren();
        if (!orders.length) {
            const empty = document.createElement("p");
            empty.className = "cart-empty";
            empty.textContent = "Your placed orders will appear here.";
            list.appendChild(empty);
            return;
        }
        orders.slice(0, 5).forEach(order => {
            const row = document.createElement("div");
            row.className = "my-order-row";
            const summary = document.createElement("span");
            summary.textContent = `#${order.id} · ${displayOrderStatus(order.orderStatus)} · ${money(order.totalAmount)}`;
            const button = document.createElement("button");
            button.type = "button";
            button.textContent = "Track";
            button.addEventListener("click", () => {
                startTracking(order.id);
                document.getElementById("order-tracking").scrollIntoView({
                    behavior: "smooth", block: "start"
                });
            });
            row.append(summary, button);
            list.appendChild(row);
        });
    } catch (error) {
        console.error("Could not load student order history:", error);
    }
}

async function loadAdminData() {
    if (!currentUser || currentUser.role !== "ADMIN") return;
    try {
        const [foodsResponse, ordersResponse] = await Promise.all([
            apiFetch("/admin/foods"),
            apiFetch("/admin/orders")
        ]);
        if (!foodsResponse.ok || !ordersResponse.ok) {
            throw new Error("Staff data could not be loaded.");
        }
        renderAdminFoods(await foodsResponse.json());
        renderAdminOrders(await ordersResponse.json());
        await loadSalesReport();
    } catch (error) {
        console.error("Staff dashboard error:", error);
        showToast(error.message || "Staff dashboard could not load.", "error");
    }
}

function renderAdminFoods(items) {
    const list = document.getElementById("admin-food-list");
    list.replaceChildren();
    items.forEach(food => {
        const row = document.createElement("div");
        row.className = "admin-food-row";
        const label = document.createElement("span");
        label.textContent =
            `${food.name} · ${money(food.price)} · ${food.available ? "Available" : "Unavailable"}`;
        const actions = document.createElement("span");
        actions.className = "admin-food-actions";

        const edit = document.createElement("button");
        edit.type = "button";
        edit.textContent = "Edit";
        edit.setAttribute("aria-expanded", "false");

        const toggle = document.createElement("button");
        toggle.type = "button";
        toggle.textContent = food.available ? "Set unavailable" : "Set available";
        toggle.addEventListener("click", async () => {
            const response = await apiFetch(`/admin/foods/${food.id}`, {
                method: "PUT",
                body: JSON.stringify({ ...food, available: !food.available })
            });
            if (!response.ok) {
                showToast(await responseError(response, "Could not update item"), "error");
                return;
            }
            await loadAdminData();
            await loadFoodMenu();
        });

        const form = document.createElement("form");
        form.className = "account-form admin-food-edit";
        form.hidden = true;
        form.setAttribute("aria-label", `Edit ${food.name}`);

        const fields = [
            { name: "name", label: "Name", type: "text", value: food.name },
            { name: "category", label: "Category", type: "text", value: food.category },
            { name: "price", label: "Price (₹)", type: "number", value: food.price },
            { name: "imageUrl", label: "Image URL", type: "url", value: food.imageUrl || "" }
        ];
        fields.forEach(field => {
            const id = `admin-food-${food.id}-${field.name}`;
            const inputLabel = document.createElement("label");
            inputLabel.htmlFor = id;
            inputLabel.textContent = field.label;

            const input = document.createElement("input");
            input.id = id;
            input.name = field.name;
            input.type = field.type;
            input.value = field.value;
            input.required = field.name !== "imageUrl";
            if (field.name === "name" || field.name === "category") {
                input.maxLength = 255;
            }
            if (field.name === "price") {
                input.min = "0";
                input.step = "0.01";
            }
            form.append(inputLabel, input);
        });

        const message = document.createElement("p");
        message.className = "form-message";
        message.setAttribute("role", "status");

        const save = document.createElement("button");
        save.type = "submit";
        save.className = "button button-primary";
        save.textContent = "Save changes";

        const cancel = document.createElement("button");
        cancel.type = "button";
        cancel.className = "button button-secondary";
        cancel.textContent = "Cancel";
        cancel.addEventListener("click", () => {
            form.hidden = true;
            edit.setAttribute("aria-expanded", "false");
        });

        const formActions = document.createElement("div");
        formActions.className = "admin-food-edit-actions";
        formActions.append(save, cancel);
        form.append(message, formActions);
        form.addEventListener("submit", async event => {
            event.preventDefault();
            const values = new FormData(form);
            const updatedFood = {
                name: values.get("name"),
                category: values.get("category"),
                price: Number(values.get("price")),
                imageUrl: values.get("imageUrl") || "",
                available: food.available
            };
            save.disabled = true;
            message.textContent = "Saving menu item...";
            try {
                const response = await apiFetch(`/admin/foods/${food.id}`, {
                    method: "PUT",
                    body: JSON.stringify(updatedFood)
                });
                if (!response.ok) {
                    throw new Error(await responseError(response, "Could not update item"));
                }
                await loadAdminData();
                await loadFoodMenu();
                showToast(`${updatedFood.name} updated.`);
            } catch (error) {
                message.textContent = error.message;
                save.disabled = false;
            }
        });

        edit.addEventListener("click", () => {
            form.hidden = !form.hidden;
            edit.setAttribute("aria-expanded", String(!form.hidden));
        });

        actions.append(edit, toggle);
        row.append(label, actions, form);
        list.appendChild(row);
    });
}

function renderAdminOrders(orders) {
    const list = document.getElementById("admin-order-list");
    list.replaceChildren();
    orders.slice(0, 12).forEach(order => {
        const row = document.createElement("div");
        row.className = "admin-food-row";
        const label = document.createElement("span");
        label.textContent = `#${order.id} · ${order.studentName} · ${money(order.totalAmount)} · ${order.orderStatus}`;
        const status = document.createElement("select");
        ["PREPARING", "READY", "COMPLETED", "CANCELLED"].forEach(value => {
            const option = document.createElement("option");
            option.value = value;
            option.textContent = value;
            status.appendChild(option);
        });
        if (order.orderStatus === "PLACED") {
            const placed = document.createElement("option");
            placed.value = "PLACED";
            placed.textContent = "PENDING";
            status.prepend(placed);
        }
        status.value = order.orderStatus;
        const update = document.createElement("button");
        update.type = "button";
        update.textContent = "Update";
        update.addEventListener("click", async () => {
            const response = await apiFetch(`/orders/${order.id}/status`, {
                method: "PATCH",
                body: JSON.stringify({ status: status.value })
            });
            if (!response.ok) {
                showToast(await responseError(response, "Could not update order"), "error");
                return;
            }
            loadAdminData();
        });
        const actions = document.createElement("span");
        actions.append(status, update);
        row.append(label, actions);
        list.appendChild(row);
    });
}

async function loadSalesReport() {
    const period = document.getElementById("report-period").value;
    const response = await apiFetch(`/admin/reports/sales?period=${period}`);
    if (!response.ok) {
        throw new Error(await responseError(response, "Could not load sales report"));
    }
    const report = await response.json();
    document.getElementById("sales-report").textContent =
        `${report.orderCount} orders · ${money(report.grossSales)} collected · ` +
        `${money(report.pendingSales)} pending (${report.from} – ${report.to})`;
}

function signOut() {
    authToken = "";
    currentUser = null;
    seenNotificationIds.clear();
    try {
        sessionStorage.removeItem("campusBiteToken");
    } catch (error) {
        console.error("Could not clear sign-in token:", error);
    }
    updateAccountViews();
    showToast("You have signed out.");
}

function addToCart(foodId) {
    const food = foods.find(item => Number(item.id) === Number(foodId));

    if (!food || food.available === false) {
        showToast("This item is currently unavailable.", "error");
        return;
    }

    const existing = cart.find(item => item.id === Number(foodId));

    if (existing) {
        existing.quantity++;
    } else {
        cart.push({
            id: Number(food.id),
            name: food.name,
            price: Number(food.price),
            quantity: 1
        });
    }

    saveCart();
    renderCart();
    renderRecommendations();
    showToast(`${food.name} added to your cart.`);
}

function changeQuantity(foodId, amount) {
    const item = cart.find(entry => entry.id === Number(foodId));
    if (!item) return;

    item.quantity += amount;

    if (item.quantity <= 0) {
        cart = cart.filter(entry => entry.id !== Number(foodId));
    }

    saveCart();
    renderCart();
}

function createQuantityButton(label, accessibleLabel, callback) {
    const button = document.createElement("button");
    button.type = "button";
    button.textContent = label;
    button.setAttribute("aria-label", accessibleLabel);
    button.addEventListener("click", callback);
    return button;
}

function renderCart() {
    cartItems.replaceChildren();

    const totalQuantity = cart.reduce(
        (sum, item) => sum + item.quantity, 0
    );

    const total = cart.reduce(
        (sum, item) => sum + item.price * item.quantity, 0
    );

    cartCount.textContent = totalQuantity;
    cartCountHeading.textContent = totalQuantity;
    cartTotal.textContent = money(total);
    checkoutButton.disabled = cart.length === 0 || isSubmitting;

    if (cart.length === 0) {
        const empty = document.createElement("p");
        empty.className = "cart-empty";
        empty.textContent =
            "Your cart is waiting for something delicious.";
        cartItems.appendChild(empty);
        return;
    }

    cart.forEach(item => {
        const row = document.createElement("div");
        row.className = "cart-row";

        const details = document.createElement("div");
        details.className = "item-details";

        const name = document.createElement("strong");
        name.textContent = item.name;

        const price = document.createElement("small");
        price.textContent = `${money(item.price)} each`;

        details.append(name, price);

        const controls = document.createElement("div");
        controls.className = "quantity-controls";

        const decrease = createQuantityButton(
            "−",
            `Remove one ${item.name}`,
            () => changeQuantity(item.id, -1)
        );

        const quantity = document.createElement("span");
        quantity.textContent = item.quantity;

        const increase = createQuantityButton(
            "+",
            `Add one ${item.name}`,
            () => changeQuantity(item.id, 1)
        );

        controls.append(decrease, quantity, increase);

        const subtotal = document.createElement("strong");
        subtotal.textContent = money(item.price * item.quantity);

        const remove = document.createElement("button");
        remove.type = "button";
        remove.className = "remove-item";
        remove.textContent = "Remove item";
        remove.setAttribute("aria-label", `Remove ${item.name} from cart`);

        remove.addEventListener("click", () => {
            cart = cart.filter(entry => entry.id !== item.id);
            saveCart();
            renderCart();
            showToast(`${item.name} removed from cart.`);
        });

        row.append(details, controls, subtotal, remove);
        cartItems.appendChild(row);
    });
}

function getCategory(food) {
    const value = String(food.category || "").trim();
    return value || "Other";
}

function renderCategories() {
    categoryList.replaceChildren();

    const allCategories = [
        "All",
        ...new Set(foods.map(getCategory))
    ];

    allCategories.forEach(category => {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "category-chip";
        button.textContent = category === "All" ? "All items" : category;
        button.dataset.category = category;

        const selected = activeCategory === category;
        button.classList.toggle("active", selected);
        button.setAttribute("aria-pressed", String(selected));

        button.addEventListener("click", () => {
            activeCategory = category;
            renderCategories();
            renderFoods();
        });

        categoryList.appendChild(button);
    });
}

function createFoodCard(food) {
    const card = document.createElement("article");
    card.className = "food-card tilt-card";

    const imageWrap = document.createElement("div");
    imageWrap.className = "food-image-wrap";

    if (food.imageUrl) {
        const image = document.createElement("img");
        image.className = "food-image";
        image.src = food.imageUrl;
        image.alt = food.name;
        image.loading = "lazy";
        image.decoding = "async";
        image.addEventListener("error", () => {
            image.remove();
            if (!imageWrap.querySelector(".food-image-fallback")) {
                const fallback = document.createElement("div");
                fallback.className = "food-image-fallback";
                fallback.textContent = "✳";
                imageWrap.prepend(fallback);
            }
        }, { once: true });
        imageWrap.appendChild(image);
    } else {
        const fallback = document.createElement("div");
        fallback.className = "food-image-fallback";
        fallback.textContent = "✳";
        imageWrap.appendChild(fallback);
    }

    const tag = document.createElement("span");
    tag.className = "food-category-tag";
    tag.textContent = getCategory(food);
    imageWrap.append(tag);

    const content = document.createElement("div");
    content.className = "food-card-content";

    const name = document.createElement("h3");
    name.textContent = food.name;

    const description = document.createElement("p");
    description.className = "food-description";
    description.textContent = "A campus favourite";

    const bottom = document.createElement("div");
    bottom.className = "food-card-bottom";

    const price = document.createElement("span");
    price.className = "food-price";
    price.textContent = money(food.price);

    const button = document.createElement("button");
    button.type = "button";
    button.className = "add-to-cart";
    button.innerHTML = "<span aria-hidden='true'>+</span> Add";
    button.setAttribute("aria-label", `Add ${food.name} to cart`);
    button.addEventListener("click", () => addToCart(food.id));

    bottom.append(price, button);
    content.append(name, description, bottom);
    card.append(imageWrap, content);
    return card;
}

function renderFoods() {
    foodList.replaceChildren();

    const filteredFoods = foods.filter(food => {
        const matchesCategory =
            activeCategory === "All" ||
            getCategory(food) === activeCategory;

        const query = searchTerm.toLowerCase();

        const matchesSearch = [
            food.name,
            food.category
        ].some(value => String(value || "").toLowerCase().includes(query));

        return matchesCategory && matchesSearch;
    });

    filteredFoods.forEach(food => foodList.appendChild(createFoodCard(food)));

    emptyState.hidden = filteredFoods.length !== 0;
    foodList.hidden = filteredFoods.length === 0;

    if (foods.length > 0) {
        menuStatus.textContent =
            `${filteredFoods.length} of ${foods.length} food items`;
    }
}

function renderRecommendations() {
    if (!recommendationList) return;

    recommendationList.replaceChildren();
    const availableIds = new Set(foods.map(food => Number(food.id)));
    const ranked = popularFoods.filter(food => availableIds.has(Number(food.id)));
    const suggestions = (ranked.length ? ranked : foods).slice(0, 4);

    suggestions.forEach(food =>
        recommendationList.appendChild(createFoodCard(food))
    );
}

function showCheckout() {
    if (cart.length === 0 || isSubmitting) return;

    confirmationSection.hidden = true;
    checkoutSection.hidden = false;
    checkoutMessage.textContent = "";
    if (currentUser) {
        const nameInput = document.getElementById("student-name");
        const phoneInput = document.getElementById("student-phone");
        if (!nameInput.value) nameInput.value = currentUser.name;
        if (!phoneInput.value) phoneInput.value = currentUser.phone;
    }

    checkoutSection.scrollIntoView({
        behavior: "smooth",
        block: "start"
    });
}

function displayOrderStatus(status) {
    const labels = {
        PLACED: "Pending",
        PREPARING: "Preparing",
        READY: "Ready for pickup",
        COMPLETED: "Completed",
        CANCELLED: "Cancelled"
    };
    return labels[String(status || "").toUpperCase()] || status || "Unknown";
}

async function refreshTrackedOrder(showUpdate = false) {
    if (!trackedOrderId || !currentUser) return;

    try {
        const [orderResponse, historyResponse] = await Promise.all([
            apiFetch(`/orders/${trackedOrderId}`),
            apiFetch(`/orders/${trackedOrderId}/history`)
        ]);

        if (orderResponse.status === 404) {
            throw new Error("We couldn't find that order. Check the number and try again.");
        }
        if (!orderResponse.ok || !historyResponse.ok) {
            throw new Error("Order status is temporarily unavailable. Please try again.");
        }

        const order = await orderResponse.json();
        const history = await historyResponse.json();
        if (!Array.isArray(history)) {
            throw new Error("The order history response was unexpected.");
        }

        const status = String(order.orderStatus || "").toUpperCase();
        if (!status) throw new Error("The order response did not include a status.");

        if (showUpdate && lastTrackedStatus && status !== lastTrackedStatus) {
            showToast(`Order #${trackedOrderId}: ${displayOrderStatus(status)}.`);
        }

        lastTrackedStatus = status;
        document.getElementById("tracked-order-id").textContent =
            `#${trackedOrderId}`;
        document.getElementById("tracked-order-status").textContent =
            displayOrderStatus(status);
        document.getElementById("tracking-status-icon").textContent =
            status === "READY" || status === "COMPLETED" ? "✓" :
                status === "CANCELLED" ? "!" : "◷";
        trackingTimeline.replaceChildren();

        history.forEach((entry, index) => {
            const item = document.createElement("li");
            item.className = index === history.length - 1
                ? "tracking-step tracking-step-current"
                : "tracking-step";

            const title = document.createElement("strong");
            title.textContent = displayOrderStatus(entry.status);
            item.appendChild(title);

            if (entry.changedAt) {
                const time = document.createElement("time");
                time.dateTime = entry.changedAt;
                const date = new Date(entry.changedAt);
                time.textContent = Number.isNaN(date.getTime())
                    ? ""
                    : date.toLocaleString();
                item.appendChild(time);
            }
            trackingTimeline.appendChild(item);
        });

        trackingResult.hidden = false;
        trackingMessage.textContent =
            status === "COMPLETED" || status === "CANCELLED"
                ? "This order is no longer being updated."
                : "Status refreshes automatically every 15 seconds while this page is open.";

        if (status === "COMPLETED" || status === "CANCELLED") {
            clearInterval(trackingTimer);
            trackingTimer = null;
        }
    } catch (error) {
        console.error("Order tracking error:", error);
        trackingMessage.textContent =
            error.message || "Could not load order status.";
        trackingResult.hidden = true;
    }
}

function startTracking(orderId, persist = true) {
    const parsedOrderId = Number(orderId);
    if (!Number.isSafeInteger(parsedOrderId) || parsedOrderId < 1) {
        trackingMessage.textContent = "Enter a valid order number.";
        return;
    }
    if (!currentUser) {
        trackingMessage.textContent = "Sign in to view your private order tracking details.";
        document.getElementById("account-section").scrollIntoView({
            behavior: "smooth", block: "start"
        });
        return;
    }

    trackedOrderId = parsedOrderId;
    lastTrackedStatus = null;
    trackingForm.elements.orderId.value = String(parsedOrderId);
    if (persist) {
        try {
            sessionStorage.setItem("campusBiteTrackedOrder", String(parsedOrderId));
        } catch (error) {
            console.error("Could not save tracked order:", error);
        }
    }

    clearInterval(trackingTimer);
    refreshTrackedOrder();
    trackingTimer = setInterval(() => {
        if (!document.hidden) refreshTrackedOrder(true);
    }, 15000);
}

function getErrorMessage(data, responseText, status) {
    if (data && typeof data === "object") {
        if (typeof data.message === "string") return data.message;
        if (typeof data.error === "string") return data.error;
    }

    if (responseText && responseText.length < 250) {
        return responseText;
    }

    return `Could not place order (HTTP ${status}). Please try again.`;
}

async function placeOrder(event) {
    event.preventDefault();

    if (isSubmitting) return;

    if (!currentUser) {
        checkoutMessage.textContent = "Sign in or create a student account before placing an order.";
        document.getElementById("account-section").scrollIntoView({
            behavior: "smooth", block: "start"
        });
        return;
    }

    if (cart.length === 0) {
        checkoutMessage.textContent = "Please add items to your cart.";
        return;
    }

    const studentName = document.getElementById("student-name").value.trim();
    const studentPhone = document.getElementById("student-phone").value.trim();

    if (!studentName || !studentPhone) {
        checkoutMessage.textContent =
            "Please enter your name and phone number.";
        return;
    }

    // Capture the current order before making the request.
    const submittedCart = cart.map(item => ({ ...item }));

    const requestBody = {
        studentName,
        studentPhone,
        paymentMethod: document.getElementById("payment-method").value,
        items: submittedCart.map(item => ({
            foodItemId: item.id,
            quantity: item.quantity
        }))
    };

    isSubmitting = true;
    placeOrderButton.disabled = true;
    placeOrderButton.textContent = "Placing your order...";
    checkoutButton.disabled = true;
    checkoutMessage.textContent = "Submitting your order...";

    try {
        const response = await apiFetch("/orders", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(requestBody)
        });

        const responseText = await response.text();
        let data = {};

        try {
            data = responseText ? JSON.parse(responseText) : {};
        } catch {
            data = {};
        }

        if (!response.ok) {
            throw new Error(
                getErrorMessage(data, responseText, response.status)
            );
        }

        const orderId = data.id ?? data.orderId ?? data.order_id;
        const paymentMethod = requestBody.paymentMethod;

        cart = [];
        saveCart();
        renderCart();
        checkoutForm.reset();

        checkoutSection.hidden = true;
        confirmationSection.hidden = false;

        const confirmationMessage =
            document.getElementById("confirmation-message");

        confirmationMessage.textContent = orderId != null
            ? `Your order ID is ${orderId}.`
            : "Your order has been submitted. Please check the order details.";

        if (orderId != null) startTracking(orderId);
        const confirmationNote = document.getElementById("confirmation-note");
        confirmationNote.textContent = paymentMethod === "COUNTER"
            ? "Please pay at the canteen counter."
            : "Online payment is being prepared.";

        confirmationSection.scrollIntoView({
            behavior: "smooth",
            block: "start"
        });

        showToast("Order placed successfully!");
        if (orderId != null && paymentMethod === "RAZORPAY") {
            try {
                await beginRazorpayCheckout(orderId);
            } catch (paymentError) {
                confirmationNote.textContent =
                    `Order #${orderId} was placed but payment is still pending. ` +
                    `${paymentError.message} You can pay at the canteen counter.`;
                showToast("Payment was not completed.", "error");
            }
        }

    } catch (error) {
        console.error("Order submission error:", error);

        checkoutMessage.textContent =
            error.message || "Could not place your order. Please try again.";

        showToast("Your order could not be placed.", "error");

    } finally {
        isSubmitting = false;
        placeOrderButton.disabled = false;
        placeOrderButton.innerHTML = 'Place my order <span>→</span>';
        checkoutButton.disabled = cart.length === 0;
    }

    async function loadRazorpayCheckout() {
        if (window.Razorpay) return;
        await new Promise((resolve, reject) => {
            const script = document.createElement("script");
            script.src = "https://checkout.razorpay.com/v1/checkout.js";
            script.onload = resolve;
            script.onerror = () => reject(new Error("Could not load the payment provider."));
            document.head.appendChild(script);
        });
        if (!window.Razorpay) {
            throw new Error("The payment provider did not initialize.");
        }
    }

    async function beginRazorpayCheckout(orderId) {
        const response = await apiFetch(`/orders/${orderId}/payment/razorpay`, {
            method: "POST"
        });
        if (!response.ok) {
            throw new Error(await responseError(response, "Could not start online payment"));
        }
        const order = await response.json();
        await loadRazorpayCheckout();

        await new Promise((resolve, reject) => {
            const checkout = new window.Razorpay({
                key: order.key,
                amount: order.amount,
                currency: order.currency,
                name: order.name,
                description: order.description,
                order_id: order.gatewayOrderId,
                prefill: { name: currentUser.name, email: currentUser.email, contact: currentUser.phone },
                theme: { color: "#24543D" },
                handler: async result => {
                    try {
                        const verifyResponse = await apiFetch(
                            `/orders/${orderId}/payment/razorpay/verify`, {
                                method: "POST",
                                body: JSON.stringify({
                                    razorpayOrderId: result.razorpay_order_id,
                                    razorpayPaymentId: result.razorpay_payment_id,
                                    razorpaySignature: result.razorpay_signature
                                })
                            });
                        if (!verifyResponse.ok) {
                            throw new Error(await responseError(
                                verifyResponse, "Payment verification failed"));
                        }
                        document.getElementById("confirmation-note").textContent =
                            "Payment successful. Your receipt is recorded securely.";
                        showToast("Payment successful!");
                        resolve();
                    } catch (error) {
                        reject(error);
                    }
                },
                modal: {
                    ondismiss: () => reject(new Error(
                        "Payment window closed before payment completed."))
                }
            });
            checkout.open();
        });
    }
}

async function loadFoodMenu() {
    menuStatus.textContent = "Loading delicious options...";

    try {
        const response = await fetch(`${API_BASE_URL}/foods`);

        if (!response.ok) {
            throw new Error(`Menu request failed (${response.status}).`);
        }

        const result = await response.json();

        if (!Array.isArray(result)) {
            throw new Error("Unexpected menu response.");
        }

        foods = result.filter(food => food.available !== false);
        try {
            const popularResponse = await apiFetch("/foods/popular");
            if (popularResponse.ok) {
                const ranked = await popularResponse.json();
                if (Array.isArray(ranked)) popularFoods = ranked;
            }
        } catch (error) {
            console.error("Popular food options could not load:", error);
        }

        // Keep only cart items that still exist in the menu.
        const availableIds = new Set(foods.map(food => Number(food.id)));

        cart = cart
            .filter(item => availableIds.has(item.id))
            .map(item => {
                const food = foods.find(entry => Number(entry.id) === item.id);

                return {
                    ...item,
                    name: food.name,
                    price: Number(food.price)
                };
            });

        saveCart();
        renderCategories();
        renderFoods();
        renderRecommendations();
        renderCart();
        document.getElementById("food-choice-count").textContent =
            `${foods.length}+`;

        menuStatus.textContent =
            `${foods.length} delicious options ready to explore`;

    } catch (error) {
        console.error("Menu loading error:", error);

        menuStatus.textContent =
            "Menu could not load. Please check the backend connection.";

        foodList.hidden = true;

        const retry = document.createElement("button");
        retry.type = "button";
        retry.className = "button button-secondary";
        retry.textContent = "Try loading again";
        retry.addEventListener("click", loadFoodMenu);

        emptyState.hidden = false;
        emptyState.querySelector("h3").textContent = "Menu unavailable";
        emptyState.querySelector("p").textContent =
            "Make sure CampusBite's backend is running, then retry.";

        const existingRetry = document.getElementById("menu-retry");
        if (existingRetry) existingRetry.remove();

        retry.id = "menu-retry";
        emptyState.appendChild(retry);
    }
}

async function checkMenuUpdates() {
    if (document.hidden || foods.length === 0) return;

    try {
        const response = await fetch(`${API_BASE_URL}/foods`);
        if (!response.ok) {
            throw new Error(`Menu refresh failed (${response.status}).`);
        }
        const result = await response.json();
        if (!Array.isArray(result)) {
            throw new Error("Unexpected menu refresh response.");
        }

        const availableFoods = result.filter(food => food.available !== false);
        const currentSnapshot = JSON.stringify(foods.map(food => ({
            id: food.id,
            name: food.name,
            category: food.category,
            price: food.price,
            imageUrl: food.imageUrl
        })));
        const latestSnapshot = JSON.stringify(availableFoods.map(food => ({
            id: food.id,
            name: food.name,
            category: food.category,
            price: food.price,
            imageUrl: food.imageUrl
        })));

        if (currentSnapshot !== latestSnapshot) {
            showToast("The menu or item availability has been updated.");
            await loadFoodMenu();
        }
    } catch (error) {
        console.error("Menu update check failed:", error);
    }
}

searchInput.addEventListener("input", () => {
    searchTerm = searchInput.value.trim();
    renderFoods();
});

document.getElementById("show-login").addEventListener("click", () => {
    document.getElementById("show-login").classList.add("active");
    document.getElementById("show-register").classList.remove("active");
    document.getElementById("login-form").hidden = false;
    document.getElementById("register-form").hidden = true;
    document.getElementById("verify-email-form").hidden = true;
});

document.getElementById("show-register").addEventListener("click", () => {
    document.getElementById("show-register").classList.add("active");
    document.getElementById("show-login").classList.remove("active");
    document.getElementById("register-form").hidden = false;
    document.getElementById("login-form").hidden = true;
    document.getElementById("verify-email-form").hidden = true;
});

document.getElementById("login-form").addEventListener("submit", async event => {
    event.preventDefault();
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    showAccountMessage("login-message", "Signing in...");
    try {
        const response = await apiFetch("/auth/login", {
            method: "POST",
            body: JSON.stringify({
                email: form.get("email"),
                password: form.get("password")
            })
        });
        if (!response.ok) throw new Error(await responseError(response, "Sign in failed"));
        await acceptAuthentication(response);
        showAccountMessage("login-message", "");
        formElement.reset();
    } catch (error) {
        showAccountMessage("login-message", error.message);
    }
});

document.getElementById("register-form").addEventListener("submit", async event => {
    event.preventDefault();
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    showAccountMessage("register-message", "Creating your student account...");
    try {
        const response = await apiFetch("/auth/register", {
            method: "POST",
            body: JSON.stringify({
                name: form.get("name"),
                phone: form.get("phone"),
                email: form.get("email"),
                password: form.get("password")
            })
        });
        if (response.status === 202) {
            const result = await response.json();
            document.getElementById("verification-email").value = result.email;
            document.getElementById("register-form").hidden = true;
            document.getElementById("verify-email-form").hidden = false;
            showAccountMessage("verification-message", result.message);
            return;
        }
        if (!response.ok) throw new Error(await responseError(response, "Registration failed"));
        await acceptAuthentication(response);
        showAccountMessage("register-message", "");
        formElement.reset();
    } catch (error) {
        showAccountMessage(
            document.getElementById("verify-email-form").hidden
                ? "register-message"
                : "verification-message",
            error.message);
    }
});

document.getElementById("verify-email-form").addEventListener("submit", async event => {
    event.preventDefault();
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    showAccountMessage("verification-message", "Verifying your email...");
    try {
        const response = await apiFetch("/auth/verify-email", {
            method: "POST",
            body: JSON.stringify({
                email: form.get("email"),
                code: form.get("code")
            })
        });
        if (!response.ok) throw new Error(await responseError(response, "Email verification failed"));
        await acceptAuthentication(response);
        formElement.reset();
        formElement.hidden = true;
        document.querySelector(".account-tabs").hidden = false;
        showAccountMessage("verification-message", "");
    } catch (error) {
        showAccountMessage("verification-message", error.message);
    }
});

document.getElementById("resend-verification").addEventListener("click", () => {
    document.getElementById("register-form").requestSubmit();
});

document.getElementById("profile-form").addEventListener("submit", async event => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const update = {
        name: form.get("name"),
        phone: form.get("phone")
    };
    const newPassword = String(form.get("newPassword") || "");
    if (newPassword) update.newPassword = newPassword;
    showAccountMessage("profile-message", "Saving your profile...");
    try {
        const response = await apiFetch("/auth/me", {
            method: "PUT",
            body: JSON.stringify(update)
        });
        if (!response.ok) throw new Error(await responseError(response, "Profile update failed"));
        currentUser = await response.json();
        document.getElementById("profile-new-password").value = "";
        document.getElementById("profile-heading").textContent =
            `Hello, ${currentUser.name}`;
        accountLink.textContent = currentUser.name.split(" ")[0] || "Account";
        showAccountMessage("profile-message", "Profile updated.");
    } catch (error) {
        showAccountMessage("profile-message", error.message);
    }
});

document.getElementById("logout-button").addEventListener("click", signOut);

notificationButton.addEventListener("click", () => {
    notificationList.hidden = !notificationList.hidden;
});

document.getElementById("report-period").addEventListener("change", loadSalesReport);

document.getElementById("admin-food-form").addEventListener("submit", async event => {
    event.preventDefault();
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    const food = {
        name: form.get("name"),
        category: form.get("category"),
        price: Number(form.get("price")),
        imageUrl: form.get("imageUrl") || "",
        available: true
    };
    try {
        const response = await apiFetch("/admin/foods", {
            method: "POST",
            body: JSON.stringify(food)
        });
        if (!response.ok) throw new Error(await responseError(response, "Could not add menu item"));
        formElement.reset();
        showAccountMessage("admin-food-message", "Menu item added.");
        await loadAdminData();
        await loadFoodMenu();
    } catch (error) {
        showAccountMessage("admin-food-message", error.message);
    }
});

trackingForm.addEventListener("submit", event => {
    event.preventDefault();
    startTracking(trackingForm.elements.orderId.value);
});

document.getElementById("clear-filters").addEventListener("click", () => {
    searchInput.value = "";
    searchTerm = "";
    activeCategory = "All";
    renderCategories();
    renderFoods();
});

checkoutButton.addEventListener("click", showCheckout);
checkoutForm.addEventListener("submit", placeOrder);

document.getElementById("new-order-button").addEventListener("click", () => {
    confirmationSection.hidden = true;
    checkoutSection.hidden = true;

    document.getElementById("menu").scrollIntoView({
        behavior: "smooth",
        block: "start"
    });
});

loadFoodMenu();
setInterval(checkMenuUpdates, 60000);
restoreAuthentication().then(() => {
    updateAccountViews();
    if (currentUser) {
        try {
            const savedOrderId = sessionStorage.getItem("campusBiteTrackedOrder");
            if (savedOrderId) startTracking(savedOrderId, false);
        } catch (error) {
            console.error("Could not restore tracked order:", error);
        }
    }
});

if ("serviceWorker" in navigator) {
    window.addEventListener("load", () => {
        navigator.serviceWorker.register("/service-worker.js")
            .catch(error => console.error("App offline support unavailable:", error));
    });
}
