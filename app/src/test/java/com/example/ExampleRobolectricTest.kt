package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun testAppNameResource() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("weatherX", appName)
  }

  @Test
  fun testWeatherParserStrictDataMapping() {
    val sampleJson = """
      {
        "latitude": 36.75,
        "longitude": 3.06,
        "current": {
          "temperature_2m": 24.5,
          "apparent_temperature": 25.1,
          "weather_code": 0,
          "is_day": 1,
          "precipitation": 0.0,
          "surface_pressure": 1018.4,
          "wind_speed_10m": 19.3,
          "wind_direction_10m": 280,
          "relative_humidity_2m": 58
        },
        "daily": {
          "time": ["2026-09-26", "2026-09-27"],
          "weather_code": [0, 1],
          "temperature_2m_max": [29.0, 27.5],
          "temperature_2m_min": [18.0, 16.5],
          "uv_index_max": [6.7, 5.8],
          "precipitation_sum": [0.0, 1.2],
          "wind_speed_10m_max": [22.0, 20.0]
        },
        "hourly": {
          "time": ["2026-09-26T12:00", "2026-09-26T13:00"],
          "temperature_2m": [24.0, 25.0],
          "weather_code": [0, 0],
          "relative_humidity_2m": [58, 55],
          "surface_pressure": [1018.4, 1018.0],
          "wind_speed_10m": [19.3, 18.5],
          "uv_index": [6.7, 6.5]
        }
      }
    """.trimIndent()

    val parsed = WeatherParser.parse(sampleJson, "Algiers", "DZ")

    // Strict 1-to-1 data mapping assertions:
    // 1. Wind speed bound ONLY to windSpeed (not humidity or moon)
    assertEquals(19.3, parsed.windSpeed, 0.01)
    assertEquals(280, parsed.windDirection)

    // 2. Pressure bound ONLY to surfacePressure
    assertEquals(1018.4, parsed.surfacePressure, 0.01)

    // 3. UV index bound ONLY to uvIndex
    assertEquals(6.7, parsed.uvIndex, 0.01)

    // 4. Humidity bound ONLY to relativeHumidity
    assertEquals(58, parsed.relativeHumidity)

    // 5. Moon phase computed independently without overriding wind/pressure/UV
    assertNotNull(parsed.moonPhase)
    assertTrue(parsed.moonIllumination >= 0 && parsed.moonIllumination <= 100)

    // 6. Hourly & Daily telemetry parsed
    assertEquals(2, parsed.hourlyList.size)
    assertEquals(19.3, parsed.hourlyList[0].windSpeed, 0.01)
    assertEquals(1018.4, parsed.hourlyList[0].surfacePressure, 0.01)
    assertEquals(6.7, parsed.hourlyList[0].uvIndex, 0.01)

    assertEquals(2, parsed.dailyList.size)
    assertEquals(29.0, parsed.dailyList[0].tempMax, 0.01)
  }
}
