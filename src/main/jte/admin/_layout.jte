@import gg.jte.Content
@import org.huebert.ncbot.config.NcbotProperties

@param String title
@param Content content
@param NcbotProperties properties = null

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ncbot admin - ${title}</title>
    <link rel="stylesheet" href="/css/admin.css">
</head>
<body>
    <nav class="navbar">
        <div class="nav-brand">ncbot admin</div>
        <div class="nav-links">
            <a href="/admin" class="${title == "Dashboard" ? "active" : ""}">Dashboard</a>
            <a href="/admin/messages" class="${title == "Messages" ? "active" : ""}">Messages</a>
            <a href="/admin/config" class="${title == "Config" ? "active" : ""}">Config</a>
        </div>
    </nav>
    <main class="container">
        ${content}
    </main>
    <footer class="footer">
        <p>ncbot - Meshcore AI Chat Provider</p>
    </footer>
</body>
</html>
