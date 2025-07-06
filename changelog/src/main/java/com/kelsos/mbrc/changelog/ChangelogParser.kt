package com.kelsos.mbrc.changelog

import android.content.Context
import android.util.Xml
import androidx.annotation.RawRes
import java.io.IOException
import java.io.InputStream
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

class ChangelogParser(private val context: Context) {
  private val ns: String? = null

  fun changelog(@RawRes resId: Int): List<ChangeLogEntry> =
    parse(context.resources.openRawResource(resId))

  @Throws(XmlPullParserException::class, IOException::class)
  private fun parse(inputStream: InputStream): List<ChangeLogEntry> {
    inputStream.use { stream ->
      val parser: XmlPullParser = Xml.newPullParser()
      parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
      parser.setInput(stream, null)
      parser.nextTag()
      return readFeed(parser)
    }
  }

  @Throws(XmlPullParserException::class, IOException::class)
  private fun readFeed(parser: XmlPullParser): List<ChangeLogEntry> {
    val entries = mutableListOf<ChangeLogEntry>()

    parser.require(XmlPullParser.START_TAG, ns, TAG_CHANGELOG)
    while (parser.next() != XmlPullParser.END_TAG) {
      if (parser.eventType != XmlPullParser.START_TAG) {
        continue
      }
      // Starts by looking for the entry tag
      if (parser.name == "version") {
        entries.addAll(readVersion(parser))
      } else {
        skip(parser)
      }
    }
    return entries
  }

  @Throws(XmlPullParserException::class, IOException::class)
  private fun readVersion(parser: XmlPullParser): List<ChangeLogEntry> {
    parser.require(XmlPullParser.START_TAG, ns, TAG_VERSION)
    val entries = mutableListOf<ChangeLogEntry>()
    val release = parser.getAttributeValue(ns, ATTRIBUTE_RELEASE)
    val version = parser.getAttributeValue(ns, ATTRIBUTE_VERSION)
    entries.add(ChangeLogEntry.Version(release, version))

    while (parser.next() != XmlPullParser.END_TAG) {
      if (parser.eventType != XmlPullParser.START_TAG) {
        continue
      }

      when (val name = parser.name) {
        TAG_BUG,
        TAG_FEATURE,
        TAG_REMOVED
        -> {
          val text = readEntry(parser, name)
          val element = ChangeLogEntry.Entry(text, getType(name))
          entries.add(element)
        }
        else -> skip(parser)
      }
    }
    return entries
  }

  // Processes summary tags in the feed.
  @Throws(IOException::class, XmlPullParserException::class)
  private fun readEntry(parser: XmlPullParser, name: String): String {
    parser.require(XmlPullParser.START_TAG, ns, name)
    val text =
      readText(parser)
        .replace("\n", " ")
        .replace("\\s+".toRegex(), " ")
        .replace("\\[".toRegex(), "<")
        .replace("]".toRegex(), ">")
    parser.require(XmlPullParser.END_TAG, ns, name)
    return text
  }

  // For the tags title and summary, extracts their text values.
  @Throws(IOException::class, XmlPullParserException::class)
  private fun readText(parser: XmlPullParser): String {
    var result = ""
    if (parser.next() == XmlPullParser.TEXT) {
      result = parser.text
      parser.nextTag()
    }
    return result
  }

  @Throws(XmlPullParserException::class, IOException::class)
  private fun skip(parser: XmlPullParser) {
    check(parser.eventType == XmlPullParser.START_TAG)
    var depth = 1
    while (depth != 0) {
      when (parser.next()) {
        XmlPullParser.END_TAG -> depth--
        XmlPullParser.START_TAG -> depth++
      }
    }
  }

  companion object {
    private const val TAG_CHANGELOG = "changelog"
    private const val TAG_VERSION = "version"
    private const val TAG_REMOVED = "removed"
    private const val TAG_BUG = "bug"
    private const val TAG_FEATURE = "feature"
    private const val ATTRIBUTE_VERSION = "version"
    private const val ATTRIBUTE_RELEASE = "release"

    fun getType(type: String): EntryType = when (type) {
      TAG_REMOVED -> EntryType.REMOVED
      TAG_FEATURE -> EntryType.FEATURE
      TAG_BUG -> EntryType.BUG
      else -> throw IllegalArgumentException("$type is not supported")
    }
  }
}
