package com.repforge.app

import com.repforge.app.data.db.NativeBuiltinCatalog
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class NativeExerciseDemoTest {
    private val exercises get() = NativeBuiltinCatalog.exercises

    @Test fun acceptedMainKeepsAll106ExercisesAnd100VideoIds() {
        assertEquals(106, exercises.size)
        val links = exercises.map { NativeExerciseDemo.resolve(it) }
        assertEquals(100, links.count { it?.kind == NativeExerciseDemo.Kind.VIDEO })
        assertEquals(6, links.count { it?.kind == NativeExerciseDemo.Kind.UNVERIFIED_SEARCH })
        assertTrue(links.none { it == null })
    }

    @Test fun videosUseOnlyCanonicalYouTubeWatchUrlsFromSourceIds() {
        for (exercise in exercises) {
            val demo = NativeExerciseDemo.resolve(exercise) ?: continue
            if (demo.kind == NativeExerciseDemo.Kind.VIDEO) {
                assertTrue(exercise.id, demo.url.matches(
                    Regex("https://www\\.youtube\\.com/watch\\?v=[A-Za-z0-9_-]{11}")))
            }
        }
    }

    @Test fun outdoorSearchIsNotInventedOrPromotedToVerifiedVideo() {
        for (id in listOf("running", "jogging", "inline-skating", "cycling", "walking", "hiking")) {
            val link = NativeExerciseDemo.resolve(exercises.first { it.id == id })!!
            assertEquals(NativeExerciseDemo.Kind.UNVERIFIED_SEARCH, link.kind)
            assertFalse(link.sourceVerified)
            assertTrue(link.url.startsWith("https://www.youtube.com/results?search_query="))
        }
    }

    @Test fun importedBuiltinWithoutDemoGetsOnlyItsAcceptedSourceReference() {
        val original = exercises.first { it.id == "db-squat" }
        val imported = original.copy(rawJson = JSONObject().put("id", "db-squat").toString(),
            videoUrl = null, videoProvider = null)
        val demo = NativeExerciseDemo.resolve(imported)
        assertEquals(NativeExerciseDemo.resolve(original), demo)
        assertEquals(NativeExerciseDemo.Kind.VIDEO, demo?.kind)
    }

    @Test fun customWithBuiltinIdCannotBorrowBuiltinsVideo() {
        val built = exercises.first { it.id == "db-squat" }
        val custom = built.copy(custom = true, rawJson = """{"id":"db-squat","custom":true}""",
            videoUrl = null, videoProvider = null)
        assertNull(NativeExerciseDemo.resolve(custom))
    }

    @Test fun customCanOnlyOpenCanonicalYoutubeVideoId() {
        val built = exercises.first()
        val safe = built.copy(custom = true,
            rawJson = """{"id":"custom","videoUrl":"https://www.youtube.com/watch?v=v_c67Omje48"}""")
        assertEquals("https://www.youtube.com/watch?v=v_c67Omje48", NativeExerciseDemo.resolve(safe)?.url)
        for (unsafe in listOf("javascript:alert(1)", "https://example.org/video",
            "https://www.youtube.com/watch?v=v_c67Omje48&evil=1",
            "http://www.youtube.com/watch?v=v_c67Omje48")) {
            assertNull(NativeExerciseDemo.resolve(safe.copy(rawJson = JSONObject().put("videoUrl", unsafe).toString())))
        }
    }

    @Test fun originalVideoCreditIsRetainedWithoutClaimingEveryLinkIsVerified() {
        val link = NativeExerciseDemo.resolve(exercises.first { it.id == "db-squat" })!!
        assertEquals("Mike Hildebrandt", link.credit)
        assertFalse(link.sourceVerified)
    }
}
