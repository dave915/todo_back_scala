package utils

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import javax.inject.{Inject, Singleton}
import play.Logger
import redis.clients.jedis.{Jedis, JedisPool}

/**
  * @author dave.th
  * @date 29/11/2018
  */
@Singleton
class JedisUtils @Inject()(private val jedisPool: JedisPool) {
  def set(key: String, obj: Object): Unit = {
    var jedis: Option[Jedis] = None
    try {
      jedis = Some(jedisPool.getResource)
      val objectMapper = new ObjectMapper() with ScalaObjectMapper
      objectMapper.registerModule(DefaultScalaModule)
      objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      val objectString = objectMapper.writeValueAsString(obj)

      jedis.get.set(key, objectString)
    } catch {
      case e : Exception => Logger.debug("redis set error : {}", e)
    } finally {
      if(jedis.nonEmpty) jedis.get.close()
    }
  }

  def get(key: String): Option[String] = {
    var jedis: Option[Jedis] = None
    var result: Option[String] = None
    try {
      jedis = Some(jedisPool.getResource)
      val redisValue = jedis.get.get(key)
      if(redisValue != null)
        result = Some(redisValue)
    } catch {
      case e : Exception => Logger.debug("redis get error : {}", e)
    } finally {
      if(jedis.nonEmpty) jedis.get.close()
    }

    result
  }

  def setWithExpire(key: String, obj: Object, second: Int): Unit = {
    var jedis: Option[Jedis] = None
    try {
      jedis = Some(jedisPool.getResource)
      val objectMapper = new ObjectMapper() with ScalaObjectMapper
      objectMapper.registerModule(DefaultScalaModule)
      objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      val objectString = objectMapper.writeValueAsString(obj)

      jedis.get.set(key, objectString)
      jedis.get.expire(key, second)
    } catch {
      case e : Exception => Logger.debug("redis setWithExpire error : {}", e)
    } finally {
      if(jedis.nonEmpty) jedis.get.close()
    }
  }
}
