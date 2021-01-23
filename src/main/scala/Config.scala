package me.amanj.zahak

import com.typesafe.config.ConfigFactory

import scala.util.Try

object Config {

  private val conf =
    ConfigFactory.load()

  val SEARCH_DEPTH =
    Try(conf.getInt("search_depth")) getOrElse 8

}
