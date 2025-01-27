//> using options -Xfatal-warnings -Wnonunit-statement -Wvalue-discard -source:3.3
import collection.ArrayOps
import collection.mutable.{ArrayBuilder, LinkedHashSet, ListBuffer}
import concurrent._
import scala.reflect.ClassTag

class C {
  import ExecutionContext.Implicits._
  def c = {
    def improved = Future(42)
    def stale = Future(27)
    improved              // error
    stale
  }
}
class D {
  def d = {
    class E
    new E().toString      // error
    new E().toString * 2
  }
}
class F {
  import ExecutionContext.Implicits._
  Future(42)              // error
}
// unused template expression uses synthetic method of class
case class K(s: String) {
  copy()                  // error
}
// mutations returning this are ok
class Mutate {
  val b = ListBuffer.empty[Int]
  b += 42                 // nowarn, returns this.type
  val xs = List(42)
  27 +: xs                // error

  def f(x: Int): this.type = this
  def g(): Unit = f(42)   // nowarn
}
// some uninteresting expressions may warn for other reasons
class WhoCares {
  null                    // error for purity
  ???                     // nowarn for impurity
}
// explicit Unit ascription to opt out of warning, even for funky applies
class Absolution {
  def f(i: Int): Int = i+1
  import ExecutionContext.Implicits._
  // Future(42): Unit        // nowarn { F(42)(ctx) }: Unit where annot is on F(42)
  // f(42): Unit             // nowarn
}
// warn uni-branched unless user disables it with -Wnonunit-if:false
class Boxed[A](a: A) {
  def isEmpty = false
  def foreach[U](f: A => U): Unit =
    if (!isEmpty) f(a)      // error (if)
  def forall(f: A => Boolean): Unit =
    if (!isEmpty) {
      println(".")
      f(a)                  // error (if)
    }
  def take(p: A => Boolean): Option[A] = {
    while (isEmpty || !p(a)) ()
    Some(a).filter(p)
  }
}
class Unibranch[A, B] {
  def runWith[U](action: B => U): A => Boolean = { x =>
    val z = null.asInstanceOf[B]
    val fellback = false
    if (!fellback) action(z)  // error (if)
    !fellback
  }
  def f(i: Int): Int = {
    def g = 17
    if (i < 42) {
      g   // error block statement
      println("uh oh")
      g   // error (if)
    }
    while (i < 42) {
      g   // error
      println("uh oh")
      g   // error
    }
    42
  }
}
class Dibranch {
  def i: Int = ???
  def j: Int = ???
  def f(b: Boolean): Int = {
    // if-expr might have an uninteresting LUB
    if (b) {          // error, at least one branch looks interesting
      println("true")
      i
    }
    else {
      println("false")
      j
    }
    42
  }
}
class Next[A] {
  val all = ListBuffer.empty[A]
  def f(it: Iterator[A], g: A => A): Unit =
    while (it.hasNext)
      all += g(it.next())   // nowarn
}
class Setting[A] {
  def set = LinkedHashSet.empty[A]
  def f(a: A): Unit = {
    set += a     // error because cannot know whether the `set` was supposed to be consumed or assigned
    println(set)
  }
}
// neither StringBuilder warns, because either append is Java method or returns this.type
// while loop looks like if branch with block1(block2, jump to label), where block2 typed as non-unit
class Strung {
  def iterator = Iterator.empty[String]
  def addString(b: StringBuilder, start: String, sep: String, end: String): StringBuilder = {
    val jsb = b.underlying
    if (start.length != 0) jsb.append(start) // error (value-discard)
    val it = iterator
    if (it.hasNext) {
      jsb.append(it.next())
      while (it.hasNext) {
        jsb.append(sep) // nowarn (java)
        jsb.append(it.next()) // error (value-discard)
      }
    }
    if (end.length != 0) jsb.append(end) // error (value-discard)
    b
  }
  def f(b: java.lang.StringBuilder, it: Iterator[String]): String = {
    while (it.hasNext) {
      b.append("\n") // nowarn (java)
      b.append(it.next()) // error (value-discard)
    }
    b.toString
  }
  def g(b: java.lang.StringBuilder, it: Iterator[String]): String = {
    while (it.hasNext) it.next()  // error
    b.toString
  }
}
class J {
  import java.util.Collections
  def xs: java.util.List[Int] = ???
  def f(): Int = {
    Collections.checkedList[Int](xs, classOf[Int])
    42
  }
}
class Variant {
  var bs = ListBuffer.empty[Int]
  val xs = ListBuffer.empty[Int]
  private[this] val ys = ListBuffer.empty[Int]
  private[this] var zs = ListBuffer.empty[Int]
  def f(i: Int): Unit = {
    bs.addOne(i)
    xs.addOne(i)
    ys.addOne(i)
    zs.addOne(i)
    println("done")
  }
}
final class ArrayOops[A](private val xs: Array[A]) extends AnyVal {
  def other: ArrayOps[A] = ???
  def transpose[B](implicit asArray: A => Array[B]): Array[Array[B]] = {
    val aClass = xs.getClass.getComponentType
    val bb = new ArrayBuilder.ofRef[Array[B]]()(ClassTag[Array[B]](aClass))
    if (xs.length == 0) bb.result()
    else {
      def mkRowBuilder() = ArrayBuilder.make[B](ClassTag[B](aClass.getComponentType))
      val bs = new ArrayOps(asArray(xs(0))).map((x: B) => mkRowBuilder())
      for (xs <- other) {
        var i = 0
        for (x <- new ArrayOps(asArray(xs))) {
          bs(i) += x
          i += 1
        }
      }
      for (b <- new ArrayOps(bs)) bb += b.result()
      bb.result()
    }
  }
}
class Depends {
  def f[A](a: A): a.type = a
  def g() = {
    val d = new Depends
    f(d)
    ()
  }
}
