
const API_BASE_URL = "/api";

const foodList = document.getElementById("food-list");
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

let foods = [];
let cart = [];
let activeCategory = "All";
let searchTerm = "";
let isSubmitting = false;

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

    filteredFoods.forEach(food => {
        const card = document.createElement("article");
        card.className = "food-card";

        const imageWrap = document.createElement("div");
        imageWrap.className = "food-image-wrap";

        const image = document.createElement("img");
        image.className = "food-image";
        image.alt = food.name;
        image.loading = "lazy";
        image.decoding = "async";

        // Use the original image URL returned by the API.
        if (food.imageUrl) {
            image.src = food.imageUrl;

            image.addEventListener("error", () => {
                image.remove();

                if (!imageWrap.querySelector(".food-image-fallback")) {
                    const fallback = document.createElement("div");
                    fallback.className = "food-image-fallback";
                    fallback.textContent = "✳";
                    fallback.setAttribute("aria-label", "Image unavailable");
                    imageWrap.prepend(fallback);
                }
            }, { once: true });
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
        if (food.imageUrl) imageWrap.prepend(image);

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
        foodList.appendChild(card);
    });

    emptyState.hidden = filteredFoods.length !== 0;
    foodList.hidden = filteredFoods.length === 0;

    if (foods.length > 0) {
        menuStatus.textContent =
            `${filteredFoods.length} of ${foods.length} food items`;
    }
}

function showCheckout() {
    if (cart.length === 0 || isSubmitting) return;

    confirmationSection.hidden = true;
    checkoutSection.hidden = false;
    checkoutMessage.textContent = "";

    checkoutSection.scrollIntoView({
        behavior: "smooth",
        block: "start"
    });
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
    checkoutMessage.textContent = "Submitting your order securely...";

    try {
        const response = await fetch(`${API_BASE_URL}/orders`, {
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

        cart = [];
        saveCart();
        renderCart();
        checkoutForm.reset();

        checkoutSection.hidden = true;
        confirmationSection.hidden = false;

        const confirmationMessage =
            document.getElementById("confirmation-message");

        confirmationMessage.textContent = orderId != null
            ? `Your order ID is ${orderId}. Please pay at the canteen counter.`
            : "Your order has been submitted. Please check the order details.";

        confirmationSection.scrollIntoView({
            behavior: "smooth",
            block: "start"
        });

        showToast("Order placed successfully!");

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
        renderCart();

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

searchInput.addEventListener("input", () => {
    searchTerm = searchInput.value.trim();
    renderFoods();
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
    checkoutSection.hidden = true();

    document.getElementById("menu").scrollIntoView({
        behavior: "smooth",
        block: "start"
    });
});

loadFoodMenu();
