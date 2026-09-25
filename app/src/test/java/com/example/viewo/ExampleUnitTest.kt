package com.example.viewo

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testPlayerAssignmentDeserialization() {
        val json = """
            {
              "isPaired": true,
              "campaign": {
                "id": 32,
                "name": "new compaign",
                "layoutType": "SPLIT",
                "splitRows": 1,
                "splitCols": 2,
                "zones": [
                  {
                    "zoneIndex": 0,
                    "row": 0,
                    "col": 0,
                    "playlist": {
                      "id": 12,
                      "name": "1",
                      "mediaItems": [
                        {
                          "id": 12,
                          "name": "Screenshot (388).png",
                          "url": "https://viewowebapplicationbackend.sfo3.cdn.digitaloceanspaces.com/media/5f255a9d-8dcb-41df-8487-05702e4548aa.png",
                          "publicUrl": "https://viewowebapplicationbackend.sfo3.cdn.digitaloceanspaces.com/media/5f255a9d-8dcb-41df-8487-05702e4548aa.png",
                          "type": "IMAGE",
                          "durationSeconds": 10
                        }
                      ]
                    }
                  },
                  {
                    "zoneIndex": 1,
                    "row": 0,
                    "col": 1,
                    "playlist": {
                      "id": 13,
                      "name": "2",
                      "mediaItems": [
                        {
                          "id": 13,
                          "name": "logo app viewo web cast.png",
                          "url": "https://viewowebapplicationbackend.sfo3.cdn.digitaloceanspaces.com/media/c2b3af4f-ff09-4f9e-a9fa-1f3d51b89180.png",
                          "publicUrl": "https://viewowebapplicationbackend.sfo3.cdn.digitaloceanspaces.com/media/c2b3af4f-ff09-4f9e-a9fa-1f3d51b89180.png",
                          "type": "IMAGE",
                          "durationSeconds": 10
                        }
                      ]
                    }
                  }
                ]
              },
              "playlist": {
                "id": 12,
                "name": "1",
                "mediaItems": [
                  {
                    "id": 12,
                    "name": "Screenshot (388).png",
                    "url": "https://viewowebapplicationbackend.sfo3.cdn.digitaloceanspaces.com/media/5f255a9d-8dcb-41df-8487-05702e4548aa.png",
                    "publicUrl": "https://viewowebapplicationbackend.sfo3.cdn.digitaloceanspaces.com/media/5f255a9d-8dcb-41df-8487-05702e4548aa.png",
                    "type": "IMAGE",
                    "durationSeconds": 10
                  }
                ]
              }
            }
        """.trimIndent()

        val gson = com.google.gson.Gson()
        val assignment = gson.fromJson(json, com.example.viewo.model.PlayerAssignment::class.java)
        assertNotNull(assignment)
        assertEquals("SPLIT", assignment.campaign?.layoutType)
        assertEquals(2, assignment.campaign?.zones?.size)
        println("Deserialization successful! Zones: ${assignment.campaign?.zones?.size}")
    }
}