package com.roomcheck.app.domain

/** A starter checklist for a room type. Users can edit everything after the room is created. */
data class RoomTemplate(
    val type: String,
    val categories: List<Pair<String, List<String>>>,
)

object DefaultTemplates {

    private val commonClutter = listOf("Floor", "Flat surfaces", "Doorway / walkway")

    val all: List<RoomTemplate> = listOf(
        RoomTemplate(
            "Kitchen",
            listOf(
                "Clutter" to listOf("Countertops", "Kitchen table", "Floor", "Sink (dishes)", "Fridge door / top"),
                "Cleanliness" to listOf("Countertops", "Sink", "Stovetop", "Microwave", "Fridge (inside)", "Floor", "Cabinet fronts"),
                "Organization" to listOf("Pantry", "Fridge contents", "Junk drawer", "Under the sink"),
                "Trash & Laundry" to listOf("Trash", "Recycling", "Dish towels"),
            ),
        ),
        RoomTemplate(
            "Bathroom",
            listOf(
                "Clutter" to listOf("Countertop", "Floor", "Shower / tub ledges"),
                "Cleanliness" to listOf("Mirror", "Countertop", "Sink", "Toilet", "Shower / tub", "Floor"),
                "Organization" to listOf("Medicine cabinet", "Under the sink", "Towels"),
                "Supplies" to listOf("Toilet paper", "Soap", "Trash"),
            ),
        ),
        RoomTemplate(
            "Bedroom",
            listOf(
                "Clutter" to listOf("Floor", "Nightstands", "Dresser top", "Chair (clothes pile)"),
                "Cleanliness" to listOf("Bed made", "Sheets fresh", "Dusting", "Floor / vacuum", "Mirror"),
                "Organization" to listOf("Closet", "Drawers", "Under the bed"),
                "Laundry" to listOf("Hamper", "Clean clothes put away"),
            ),
        ),
        RoomTemplate(
            "Living Room",
            listOf(
                "Clutter" to listOf("Coffee table", "Couch", "Floor", "Shelves", "Entertainment center"),
                "Cleanliness" to listOf("Dusting", "Floor / vacuum", "Couch cushions", "Windows", "Electronics / screens"),
                "Organization" to listOf("Blankets & pillows", "Remotes & cables", "Toys / games", "Books & magazines"),
            ),
        ),
        RoomTemplate(
            "Dining Room",
            listOf(
                "Clutter" to listOf("Table", "Chairs", "Sideboard / buffet", "Floor"),
                "Cleanliness" to listOf("Table surface", "Chairs", "Floor", "Light fixture"),
            ),
        ),
        RoomTemplate(
            "Home Office",
            listOf(
                "Clutter" to listOf("Desk", "Floor", "Shelves"),
                "Cleanliness" to listOf("Desk surface", "Keyboard & mouse", "Screen", "Floor / vacuum", "Dusting"),
                "Organization" to listOf("Paperwork / mail", "Cables", "Supplies drawer"),
            ),
        ),
        RoomTemplate(
            "Laundry Room",
            listOf(
                "Clutter" to listOf("Floor", "Folding surface", "Top of machines"),
                "Cleanliness" to listOf("Washer (inside & seal)", "Dryer lint trap", "Floor", "Sink"),
                "Laundry" to listOf("Dirty laundry backlog", "Clean laundry folded"),
                "Supplies" to listOf("Detergent", "Cleaning supplies"),
            ),
        ),
        RoomTemplate(
            "Entryway",
            listOf(
                "Clutter" to listOf("Shoes", "Coats & bags", "Mail / keys spot", "Floor"),
                "Cleanliness" to listOf("Floor", "Door mat", "Front door", "Mirror"),
            ),
        ),
        RoomTemplate(
            "Garage",
            listOf(
                "Clutter" to listOf("Floor", "Workbench", "Walkways"),
                "Cleanliness" to listOf("Floor sweep", "Cobwebs"),
                "Organization" to listOf("Tools", "Shelving / bins", "Sports & outdoor gear"),
                "Trash" to listOf("Trash & recycling bins", "Donation pile"),
            ),
        ),
        RoomTemplate(
            "Kids' Room",
            listOf(
                "Clutter" to listOf("Floor", "Bed", "Desk", "Dresser top"),
                "Cleanliness" to listOf("Bed made", "Floor / vacuum", "Dusting"),
                "Organization" to listOf("Toys", "Books", "Closet", "Drawers"),
                "Laundry" to listOf("Hamper", "Clean clothes put away"),
            ),
        ),
    )

    /** A generic checklist used when the user picks "Custom". */
    val custom = RoomTemplate(
        "Custom",
        listOf(
            "Clutter" to commonClutter,
            "Cleanliness" to listOf("Floor", "Surfaces / dusting"),
        ),
    )

    /** Rooms created automatically the first time the app is opened. */
    val starterRooms = listOf("Kitchen", "Bathroom", "Bedroom", "Living Room")

    fun forType(type: String): RoomTemplate = all.firstOrNull { it.type == type } ?: custom
}
