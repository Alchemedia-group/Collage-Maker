package com.alchemedia.collage.model

data class EmojiCategory(val id: String, val title: String, private val raw: String) {
    val items: List<String> get() = raw.split(' ').filter { it.isNotBlank() }
}

object EmojiCatalog {
    val categories = listOf(
        EmojiCategory("smile", "Smileys", "😀 😁 😂 🤣 😊 😍 🥰 😘 😎 🤩 🥳 😜 🤪 😇 😴 🤔 🙃 😭 🥺 😡 🤗 😏 🙌 👏 👍 ✌️ 🤞 🙏"),
        EmojiCategory("love", "Love", "❤️ 🧡 💛 💚 💙 💜 🖤 🤍 💖 💕 💘 💝 💞 💓 ✨ 💋 🌹 🫶 😻 💌"),
        EmojiCategory("party", "Party", "🎉 🎊 🎂 🎈 🎁 🥂 🍾 🪩 🎶 🎤 🎸 🏆 🥇 🎯 🎀 🎄 🎃 🕯️ 🎆 🧁"),
        EmojiCategory("nature", "Nature", "🌸 🌼 🌻 🌹 🌷 🍀 🌿 🌴 🌵 🍁 🍂 🌈 ☀️ 🌙 ⭐ 🌟 ⚡ 🔥 💧 ❄️ 🌊 ☁️"),
        EmojiCategory("animals", "Animals", "🐶 🐱 🐼 🦊 🐰 🐻 🐨 🐯 🦁 🐸 🐵 🦄 🐝 🦋 🐢 🐬 🐧 🐥 🦖 🐙"),
        EmojiCategory("food", "Food", "🍕 🍔 🍟 🌮 🍣 🍩 🍪 🍰 🍓 🍒 🍉 🍍 🥑 🍷 🍺 ☕ 🧋 🍦 🍿 🥐"),
        EmojiCategory("travel", "Travel", "✈️ 🚗 🏖️ 🏝️ 🗻 🏔️ ⛺ 🗼 🌅 🌃 📸 🎒 🧳 🗺️ 🚲 🛶 🏠 🎡"),
        EmojiCategory("symbols", "Symbols", "✔️ ✖️ ➕ 💯 🔔 📍 🏷️ 💬 💭 👑 💎 🔮 🚫 ♾️ ❓ ❗ 🔝 🆒"),
    )

    /** Shape stickers use the same paths as the layout shapes and are recoloured by the user. */
    val shapeStickers = listOf("heart", "star", "circle", "diamond", "hexagon", "triangle", "leaf", "arch")
}
