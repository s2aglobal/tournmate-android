package com.s2aglobal.tournmate.ui.screen.discover

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.graphics.vector.ImageVector
import com.s2aglobal.tournmate.domain.model.SportType

/**
 * A single link card displayed in the Discover tab.
 * Links open inside the app via Chrome Custom Tabs.
 */
data class DiscoverLink(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val source: String,
    val url: String,
)

/**
 * Curated links for the Discover tab — news, live scores, rankings, and highlights.
 * Content adapts to the user's preferred sport (mirrors iOS DiscoverData).
 */
object DiscoverData {

    // ─── Latest News ────────────────────────────────
    fun latestNews(sport: SportType): List<DiscoverLink> = when (sport) {
        SportType.PICKLEBALL -> listOf(
            DiscoverLink("PPA Tour News", "Latest from the Professional Pickleball Association.", Icons.Filled.Newspaper, "PPA", "https://www.ppatour.com/news/"),
            DiscoverLink("The Dink", "Pickleball news, stories, and analysis.", Icons.Filled.Description, "The Dink", "https://www.thedinkpickleball.com/"),
            DiscoverLink("USA Pickleball", "Official governing body news and updates.", Icons.Filled.FormatQuote, "USAP", "https://usapickleball.org/news/"),
        )
        SportType.TENNIS -> listOf(
            DiscoverLink("ATP Tour News", "Official news from the ATP Tour.", Icons.Filled.Newspaper, "ATP", "https://www.atptour.com/en/news"),
            DiscoverLink("WTA News", "Official news from the WTA.", Icons.Filled.Description, "WTA", "https://www.wtatennis.com/news"),
            DiscoverLink("Tennis.com", "In-depth tennis coverage and analysis.", Icons.Filled.FormatQuote, "Tennis", "https://www.tennis.com/news/"),
            DiscoverLink("ITF Tennis", "International Tennis Federation updates.", Icons.Filled.LocalFireDepartment, "ITF", "https://www.itftennis.com/en/news/"),
        )
        else -> listOf(
            DiscoverLink("BWF News & Press", "Official news from the Badminton World Federation.", Icons.Filled.Newspaper, "BWF", "https://bwfbadminton.com/news/"),
            DiscoverLink("ESPN Badminton", "Latest articles, match reports and analysis.", Icons.Filled.Description, "ESPN", "https://www.espn.com/badminton/"),
            DiscoverLink("Olympics Badminton", "Olympic badminton news, results, and athlete profiles.", Icons.Filled.LocalFireDepartment, "Olympics", "https://olympics.com/en/sports/badminton/"),
            DiscoverLink("Badminton Gazette", "Community stories, interviews & gear reviews.", Icons.Filled.FormatQuote, "Gazette", "https://www.badmintonplanet.com/news/"),
        )
    }

    // ─── Live Matches ───────────────────────────────
    fun liveMatches(sport: SportType): List<DiscoverLink> = when (sport) {
        SportType.PICKLEBALL -> listOf(
            DiscoverLink("PPA Live Scores", "Real-time scores from PPA Tour events.", Icons.Filled.Sensors, "PPA", "https://www.ppatour.com/schedule/"),
            DiscoverLink("Pickleball Brackets", "Live brackets and draw sheets.", Icons.Filled.ListAlt, "Pickleball", "https://www.pickleballtournaments.com/"),
        )
        SportType.TENNIS -> listOf(
            DiscoverLink("ATP Live Scores", "Real-time scores from ATP Tour events.", Icons.Filled.Sensors, "ATP", "https://www.atptour.com/en/scores/current"),
            DiscoverLink("WTA Live Scores", "Real-time scores from WTA events.", Icons.Filled.Sensors, "WTA", "https://www.wtatennis.com/scores"),
            DiscoverLink("Grand Slam Tracker", "Live draws and results for Grand Slams.", Icons.Filled.ListAlt, "Tennis", "https://www.tennis.com/scores/"),
        )
        else -> listOf(
            DiscoverLink("BWF Live Scores", "Real-time scores from BWF World Tour events.", Icons.Filled.Sensors, "BWF", "https://bwf.tournamentsoftware.com/"),
            DiscoverLink("Tournament Software", "Draw sheets, schedules, and results for all BWF events.", Icons.Filled.ListAlt, "BWF", "https://bwf.tournamentsoftware.com/ranking/ranking.aspx"),
            DiscoverLink("Badminton Live Stream", "Watch live badminton matches on BWF TV.", Icons.Filled.LiveTv, "BWF TV", "https://bfreetv.com/home"),
        )
    }

    // ─── World Rankings ─────────────────────────────
    fun worldRankings(sport: SportType): List<DiscoverLink> = when (sport) {
        SportType.PICKLEBALL -> listOf(
            DiscoverLink("PPA Rankings", "Professional Pickleball Association player rankings.", Icons.Filled.BarChart, "PPA", "https://www.ppatour.com/player-rankings/"),
            DiscoverLink("DUPR Ratings", "Dynamic Universal Pickleball Rating system.", Icons.Filled.Person, "DUPR", "https://www.dupr.com/"),
        )
        SportType.TENNIS -> listOf(
            DiscoverLink("ATP Singles Rankings", "Current ATP world ranking — Men's Singles.", Icons.Filled.Person, "ATP", "https://www.atptour.com/en/rankings/singles"),
            DiscoverLink("WTA Singles Rankings", "Current WTA world ranking — Women's Singles.", Icons.Filled.Person, "WTA", "https://www.wtatennis.com/rankings/singles"),
            DiscoverLink("ATP Doubles Rankings", "Current ATP world ranking — Men's Doubles.", Icons.Filled.Group, "ATP", "https://www.atptour.com/en/rankings/doubles"),
            DiscoverLink("WTA Doubles Rankings", "Current WTA world ranking — Women's Doubles.", Icons.Filled.Group, "WTA", "https://www.wtatennis.com/rankings/doubles"),
        )
        else -> listOf(
            DiscoverLink("Men's Singles Rankings", "Current BWF world ranking — Men's Singles.", Icons.Filled.Person, "BWF", "https://bwfbadminton.com/rankings/2/bwf-world-rankings-men-singles/"),
            DiscoverLink("Women's Singles Rankings", "Current BWF world ranking — Women's Singles.", Icons.Filled.Person, "BWF", "https://bwfbadminton.com/rankings/3/bwf-world-rankings-women-singles/"),
            DiscoverLink("Men's Doubles Rankings", "Current BWF world ranking — Men's Doubles.", Icons.Filled.Group, "BWF", "https://bwfbadminton.com/rankings/4/bwf-world-rankings-men-doubles/"),
            DiscoverLink("Women's Doubles Rankings", "Current BWF world ranking — Women's Doubles.", Icons.Filled.Group, "BWF", "https://bwfbadminton.com/rankings/5/bwf-world-rankings-women-doubles/"),
            DiscoverLink("Mixed Doubles Rankings", "Current BWF world ranking — Mixed Doubles.", Icons.Filled.Group, "BWF", "https://bwfbadminton.com/rankings/6/bwf-world-rankings-mixed-doubles/"),
        )
    }

    // ─── Highlights & Videos ────────────────────────
    fun highlights(sport: SportType): List<DiscoverLink> = when (sport) {
        SportType.PICKLEBALL -> listOf(
            DiscoverLink("PPA YouTube", "Match highlights and interviews from PPA Tour.", Icons.Filled.PlayCircle, "YouTube", "https://www.youtube.com/@PPAtour"),
            DiscoverLink("Pickleball Channel", "Tips, drills, and pro match analysis.", Icons.Filled.AutoAwesome, "YouTube", "https://www.youtube.com/@PickleballChannel"),
        )
        SportType.TENNIS -> listOf(
            DiscoverLink("Tennis TV", "Official ATP Tour match highlights.", Icons.Filled.PlayCircle, "YouTube", "https://www.youtube.com/@TennisTV"),
            DiscoverLink("WTA Highlights", "Best rallies and match recaps from WTA.", Icons.Filled.AutoAwesome, "YouTube", "https://www.youtube.com/@WTA"),
            DiscoverLink("Top Tennis Training", "Pro techniques, drills, and analysis.", Icons.Filled.Videocam, "YouTube", "https://www.youtube.com/@TopTennisTraining"),
        )
        else -> listOf(
            DiscoverLink("BWF YouTube Channel", "Match highlights, rallies of the day & interviews.", Icons.Filled.PlayCircle, "YouTube", "https://www.youtube.com/@BWF"),
            DiscoverLink("Best Rallies Collection", "Top rallies and trick shots from major tournaments.", Icons.Filled.AutoAwesome, "YouTube", "https://www.youtube.com/@BWF/playlists"),
            DiscoverLink("Shuttle Amazing", "Fan highlights, training tips & analysis.", Icons.Filled.Videocam, "YouTube", "https://www.youtube.com/@ShuttleAmazing"),
        )
    }

    // ─── Quick Links (top chips) ────────────────────
    fun quickLinks(sport: SportType): List<DiscoverLink> = when (sport) {
        SportType.PICKLEBALL -> listOf(
            DiscoverLink("PPA", "", Icons.Filled.Public, "PPA", "https://www.ppatour.com/"),
            DiscoverLink("Live Scores", "", Icons.Filled.Sensors, "PPA", "https://www.ppatour.com/schedule/"),
            DiscoverLink("DUPR", "", Icons.Filled.BarChart, "DUPR", "https://www.dupr.com/"),
            DiscoverLink("USA PB", "", Icons.Filled.Flag, "USAP", "https://usapickleball.org/"),
        )
        SportType.TENNIS -> listOf(
            DiscoverLink("ATP", "", Icons.Filled.Public, "ATP", "https://www.atptour.com/"),
            DiscoverLink("WTA", "", Icons.Filled.Public, "WTA", "https://www.wtatennis.com/"),
            DiscoverLink("Live Scores", "", Icons.Filled.Sensors, "ATP", "https://www.atptour.com/en/scores/current"),
            DiscoverLink("Rankings", "", Icons.Filled.BarChart, "ATP", "https://www.atptour.com/en/rankings/singles"),
        )
        else -> listOf(
            DiscoverLink("BWF", "", Icons.Filled.Public, "BWF", "https://bwfbadminton.com/"),
            DiscoverLink("Live Scores", "", Icons.Filled.Sensors, "BWF", "https://bwf.tournamentsoftware.com/"),
            DiscoverLink("Rankings", "", Icons.Filled.BarChart, "BWF", "https://bwfbadminton.com/rankings/"),
            DiscoverLink("BWF TV", "", Icons.Filled.LiveTv, "BWF", "https://bfreetv.com/home"),
            DiscoverLink("Olympics", "", Icons.Filled.LocalFireDepartment, "Olympics", "https://olympics.com/en/sports/badminton/"),
        )
    }
}
