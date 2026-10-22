
const API_BASE_URL = "/api";

const foodList = document.getElementById("food-list");
const menuStatus = document.getElementById("menu-status");

let foods = [];
let cart = JSON.parse(localStorage.getItem("campusBiteCart") || "[]");

function saveCart() {
    localStorage.setItem("campusBiteCart", JSON.stringify(cart));
}

function addToCart(foodId) {
    const food = foods.find(item => item.id === foodId);

    if (!food) return;

    const existingItem = cart.find(item => item.id === foodId);

    if (existingItem) {
        existingItem.quantity += 1;
    } else {
        cart.push({
            id: food.id,
            name: food.name,
            price: Number(food.price),
            quantity: 1
        });
    }

    saveCart();
    renderCart();
}

function changeQuantity(foodId, amount) {
    const item = cart.find(item => item.id === foodId);

    if (!item) return;

    item.quantity += amount;

    if (item.quantity <= 0) {
        cart = cart.filter(item => item.id !== foodId);
    }

    saveCart();
    renderCart();
}

function renderCart() {
    let cartSection = document.getElementById("cart-section");

    if (!cartSection) {
        cartSection = document.createElement("section");
        cartSection.id = "cart-section";
        cartSection.innerHTML = `
            <h2>Your Cart</h2>
            <div id="cart-items"></div>
            <p id="cart-total"></p>
        `;

        document.querySelector("main").appendChild(cartSection);
    }

    const cartItems = document.getElementById("cart-items");
    const cartTotal = document.getElementById("cart-total");

    cartItems.innerHTML = "";

    if (cart.length === 0) {
        cartItems.textContent = "Your cart is empty.";
    }

    cart.forEach(item => {
        const row = document.createElement("div");
        row.className = "cart-row";

        const details = document.createElement("span");
        details.textContent =
            `${item.name} — ₹${item.price} × ${item.quantity}`;

        const decrease = document.createElement("button");
        decrease.textContent = "−";
        decrease.setAttribute("aria-label", `Remove one ${item.name}`);
        decrease.addEventListener("click", () => changeQuantity(item.id, -1));

        const increase = document.createElement("button");
        increase.textContent = "+";
        increase.setAttribute("aria-label", `Add one ${item.name}`);
        increase.addEventListener("click", () => changeQuantity(item.id, 1));

        row.append(details, decrease, increase);
        cartItems.appendChild(row);
    });

    const total = cart.reduce(
        (sum, item) => sum + item.price * item.quantity,
        0
    );

    cartTotal.textContent = `Total: ₹${total.toFixed(2)}`;
}

async function loadFoodMenu() {
    menuStatus.textContent = "Loading food menu...";

    try {
        const response = await fetch(`${API_BASE_URL}/foods`);

        if (!response.ok) {
            throw new Error("Unable to load food menu.");
        }

        foods = await response.json();
        foods = foods.filter(food => food.available);

        foodList.innerHTML = "";

        foods.forEach(food => {
            const card = document.createElement("article");
            card.className = "food-card";

            const image = document.createElement("img");
            image.src = food.imageUrl || "";
            image.alt = food.name;
            image.loading = "lazy";
            image.onerror = () => {
                image.style.display = "none";
            };

            const content = document.createElement("div");
            content.className = "food-card-content";

            const name = document.createElement("h3");
            name.textContent = food.name;

            const category = document.createElement("p");
            category.textContent = food.category || "Campus Food";

            const price = document.createElement("div");
            price.className = "food-price";
            price.textContent = `₹${Number(food.price).toFixed(2)}`;

            const button = document.createElement("button");
            button.className = "add-to-cart";
            button.textContent = "Add to Cart";
            button.addEventListener("click", () => addToCart(food.id));

            content.append(name, category, price, button);
            card.append(image, content);
            foodList.appendChild(card);
        });

        menuStatus.textContent = `Showing ${foods.length} available food items`;

        renderCart();

    } catch (error) {
        console.error("Menu loading error:", error);
        menuStatus.textContent =
            "Unable to load menu. Please check that the backend is running.";
    }
}

loadFoodMenu();
