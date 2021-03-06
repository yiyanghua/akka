/**
 * Copyright (C) 2016-2017 Lightbend Inc. <http://www.lightbend.com/>
 */
package akka.actor.typed
package internal

import akka.{ actor ⇒ a, event ⇒ e }

import scala.concurrent._
import com.typesafe.config.ConfigFactory
import java.util.concurrent.ThreadFactory

import akka.event.Logging
import akka.event.typed.{ BusLogging, DefaultLoggingFilter, EventStream }
import akka.event.typed.{ BusLogging, DefaultLoggingFilter }
import akka.util.Timeout

private[typed] class ActorSystemStub(val name: String)
  extends ActorSystem[Nothing] with ActorRef[Nothing] with ActorRefImpl[Nothing] {

  override val path: a.ActorPath = a.RootActorPath(a.Address("akka", name)) / "user"

  override val settings: Settings = new Settings(getClass.getClassLoader, ConfigFactory.empty, name)

  override def tell(msg: Nothing): Unit = throw new RuntimeException("must not send message to ActorSystemStub")

  override def isLocal: Boolean = true
  override def sendSystem(signal: akka.actor.typed.internal.SystemMessage): Unit =
    throw new RuntimeException("must not send SYSTEM message to ActorSystemStub")

  val deadLettersInbox = new DebugRef[Any](path.parent / "deadLetters", true)
  override def deadLetters[U]: akka.actor.typed.ActorRef[U] = deadLettersInbox

  val controlledExecutor = new ControlledExecutor
  implicit override def executionContext: scala.concurrent.ExecutionContextExecutor = controlledExecutor
  override def dispatchers: akka.actor.typed.Dispatchers = new Dispatchers {
    def lookup(selector: DispatcherSelector): ExecutionContextExecutor = controlledExecutor
    def shutdown(): Unit = ()
  }

  override def dynamicAccess: a.DynamicAccess = new a.ReflectiveDynamicAccess(getClass.getClassLoader)
  override def eventStream: EventStream = new EventStream {
    override def subscribe[T](subscriber: ActorRef[T], to: Class[T]) = false
    override def setLogLevel(loglevel: Logging.LogLevel): Unit = {}
    override def logLevel = Logging.InfoLevel
    override def unsubscribe[T](subscriber: ActorRef[T], from: Class[T]) = false
    override def unsubscribe[T](subscriber: ActorRef[T]): Unit = {}
    override def publish[T](event: T): Unit = {}
  }
  override def logFilter: e.LoggingFilter = new DefaultLoggingFilter(settings, eventStream)
  override def log: e.LoggingAdapter = new BusLogging(eventStream, path.parent.toString, getClass, logFilter)
  override def logConfiguration(): Unit = log.info(settings.toString)

  override def scheduler: a.Scheduler = throw new UnsupportedOperationException("no scheduler")

  private val terminationPromise = Promise[Terminated]
  override def terminate(): Future[akka.actor.typed.Terminated] = {
    terminationPromise.trySuccess(Terminated(this)(null))
    terminationPromise.future
  }
  override def whenTerminated: Future[akka.actor.typed.Terminated] = terminationPromise.future
  override val startTime: Long = System.currentTimeMillis()
  override def uptime: Long = System.currentTimeMillis() - startTime
  override def threadFactory: java.util.concurrent.ThreadFactory = new ThreadFactory {
    override def newThread(r: Runnable): Thread = new Thread(r)
  }

  override def printTree: String = "no tree for ActorSystemStub"

  def systemActorOf[U](behavior: Behavior[U], name: String, props: Props)(implicit timeout: Timeout): Future[ActorRef[U]] = {
    Future.failed(new UnsupportedOperationException("ActorSystemStub cannot create system actors"))
  }

  def registerExtension[T <: Extension](ext: ExtensionId[T]): T =
    throw new UnsupportedOperationException("ActorSystemStub cannot register extensions")

  def extension[T <: Extension](ext: ExtensionId[T]): T =
    throw new UnsupportedOperationException("ActorSystemStub cannot register extensions")

  def hasExtension(ext: ExtensionId[_ <: Extension]): Boolean =
    throw new UnsupportedOperationException("ActorSystemStub cannot register extensions")
}
