package dev.assemble.app.core.data.remote

import dev.assemble.app.core.model.MessageAuthor
import dev.assemble.app.core.model.MessageStatus
import dev.assemble.app.core.model.Origin
import dev.assemble.app.core.model.PowerFamily
import dev.assemble.app.core.model.Preferences
import dev.assemble.app.core.model.ProfileAccent
import dev.assemble.app.core.model.ProfileStyle
import dev.assemble.app.core.model.Style
import dev.assemble.app.core.model.Team
import dev.assemble.app.core.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreMappingTest {

    @Test
    fun userDocument_missing_usesDefaultsAndName() {
        val document = userDocument(null, defaultName = "tony")
        assertEquals("tony", document.profile.name)
        assertEquals(Preferences.Any, document.preferences)
        assertFalse(document.onboardingCompleted)
        assertFalse(document.deactivated)
    }

    @Test
    fun userDocument_readsStatusOnboardingAndIgnoresUnknownTraits() {
        val document = userDocument(
            mapOf(
                "displayName" to "Pepper",
                "onboardingCompletedAt" to TestNow,
                "status" to USER_STATUS_DEACTIVATED,
                "preferences" to mapOf("origins" to listOf("Mutant", "Kryptonian"), "fame" to 0.25),
            ),
            defaultName = "tony",
        )
        assertEquals("Pepper", document.profile.name)
        assertTrue(document.onboardingCompleted)
        assertTrue(document.deactivated)
        assertEquals(setOf(Origin.Mutant), document.preferences.origins)
        assertEquals(0.25f, document.preferences.fame)
    }

    @Test
    fun preferencesAndProfile_roundTripThroughFields() {
        val preferences = Preferences(
            origins = setOf(Origin.Alien, Origin.Human),
            powers = setOf(PowerFamily.Flight),
            teams = setOf(Team.XMen),
            styles = setOf(Style.Humor),
            fame = 0.5f,
        )
        val profile = UserProfile("Kamala", "Jersey City", 3, ProfileStyle(accent = ProfileAccent.entries.last(), promptAnswer = "Embiggen"))
        val read = userDocument(profileFields(profile) + mapOf("preferences" to preferencesFields(preferences)), "x")
        assertEquals(preferences, read.preferences)
        assertEquals(profile, read.profile)
    }

    @Test
    fun photo_isWrittenReadAndRemoved() {
        val withPhoto = UserProfile("Kamala", "", 1, photo = "AAAA")
        val fields = profileFields(withPhoto)
        assertEquals("AAAA", fields["avatarPhoto"])
        assertEquals("AAAA", userDocument(fields, "x").profile.photo)

        val removed = profileFields(withPhoto.copy(photo = null), previousPhoto = "AAAA")
        assertEquals(DeleteField, removed["avatarPhoto"])

        // Sem foto antes nem agora, o campo nem entra: não depende das regras novas do Firestore.
        assertFalse(profileFields(withPhoto.copy(photo = null)).containsKey("avatarPhoto"))
    }

    @Test
    fun photo_isIgnoredWhenBlankOrTooLarge() {
        assertNull(userDocument(mapOf("avatarPhoto" to " "), "x").profile.photo)
        assertNull(userDocument(mapOf("avatarPhoto" to "A".repeat(AVATAR_PHOTO_MAX_CHARS + 1)), "x").profile.photo)
    }

    @Test
    fun matchDocument_requiresNameAndCreatedAt() {
        assertNull(matchDocument(Document("wolverine", mapOf("createdAt" to TestNow))))
        assertNull(matchDocument(Document("wolverine", mapOf("characterName" to "Wolverine"))))
    }

    @Test
    fun matchDocument_readsConnectionFields() {
        val match = matchDocument(
            matchDoc(
                "storm",
                name = "Storm",
                extra = mapOf(
                    "characterNamePtBR" to "Tempestade",
                    "profileUnlockSeenAt" to TestNow,
                    "suggestions" to listOf("Oi", 3),
                    "hidden" to true,
                ),
            ),
        )!!
        assertEquals("storm", match.connection.characterId)
        assertEquals(80, match.connection.score)
        assertTrue(match.connection.profileUnlockSeen)
        assertEquals("Tempestade", match.characterNamePtBR)
        assertEquals(listOf("Oi"), match.suggestions)
        assertTrue(match.hidden)
    }

    @Test
    fun messageFrom_hiddenSkipped_blockedUserFlagged_readByLastReadAt() {
        val hidden = Document("m0", mapOf("createdAt" to TestNow, "author" to "USER", "hidden" to true))
        assertNull(messageFrom(hidden, "storm", null))

        val blocked = messageFrom(Document("m1", mapOf("createdAt" to TestNow, "author" to "USER", "text" to "", "blocked" to true)), "storm", null)!!
        assertEquals(MessageAuthor.User, blocked.author)
        assertEquals(MessageStatus.Blocked, blocked.status)

        val reply = Document("m2", mapOf("createdAt" to TestNow, "author" to "CHARACTER", "text" to "Olá"))
        assertFalse(messageFrom(reply, "storm", lastReadAt = null)!!.read)
        assertTrue(messageFrom(reply, "storm", lastReadAt = TestNow)!!.read)
    }
}
