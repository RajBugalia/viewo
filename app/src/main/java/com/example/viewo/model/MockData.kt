package com.example.viewo.model

object MockData {
    val screens = listOf(
        Screen("S01", "Main Entrance", "Building A", ScreenStatus.ONLINE, "10:42 AM", "Summer Sale"),
        Screen("S02", "Reception", "Building A", ScreenStatus.OFFLINE, "09:35 AM", null),
        Screen("S03", "Cafeteria", "Building B", ScreenStatus.ONLINE, "10:41 AM", "Lunch Menu"),
        Screen("S04", "Elevator Lobby", "Building A", ScreenStatus.WARNING, "10:30 AM", "Summer Sale")
    )

    val campaigns = listOf(
        Campaign("C01", "Summer Sale", "Promoting summer discounts", "P01", "2026-06-01", "2026-08-31", "LANDSCAPE", listOf("S01", "S04"), CampaignStatus.ACTIVE),
        Campaign("C02", "Lunch Menu", "Daily cafeteria menu", "P02", "2026-01-01", "2026-12-31", "PORTRAIT", listOf("S03"), CampaignStatus.ACTIVE),
        Campaign("C03", "Welcome Video", "Corporate welcome video", "P03", "2026-09-01", "2026-12-31", "LANDSCAPE", listOf("S02"), CampaignStatus.SCHEDULED)
    )

    val mediaItems = listOf(
        Media("M01", "summer_offer.mp4", MediaType.VIDEO, "http://mockurl/summer.mp4", null, 30),
        Media("M02", "product.jpg", MediaType.IMAGE, "http://mockurl/product.jpg", null, null),
        Media("M03", "menu_monday.jpg", MediaType.IMAGE, "http://mockurl/menu.jpg", null, null)
    )

    val playlists = listOf(
        Playlist("P01", "Summer Sale Playlist", listOf(mediaItems[0], mediaItems[1])),
        Playlist("P02", "Cafeteria Menu", listOf(mediaItems[2]))
    )
}
