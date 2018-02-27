package scaloi
package syntax

import scalaz.{Monoid, Order, Semigroup, \/}
import scalaz.std.option._
import scalaz.syntax.std.option._

import scala.concurrent.Future
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

/**
  * Enhancements on options.
  * @param self the option value
  * @tparam A the option type
  */
final class OptionOps[A](val self: Option[A]) extends AnyVal {
  import AnyOps._
  import OptionOps.{ minMonoid, maxMonoid }

  /**
    * Flatmap over a function to a nullable value.
    * @param f the mapping function
    * @tparam B the result type
    * @return the resulting option
    */
  @inline def flatOpt[B >: Null](f: A => B): Option[B] =
    self.flatMap(a => Option(f(a)))

  /**
    * Convert an option into a successful future, if present, else a supplied failure.
    * @param e the exception if this option is absent
    * @return an available future
    */
  @inline def toFuture(e: => Exception): Future[A] =
    self.fold(Future.failed[A](e))(Future.successful)

  /**
    * Return this option, if it does not contain the specified value, else None.
    * @param a the value to remove
    * @return this option without the specified value
    */
  @inline def -(a: A): Option[A] = self.filter(_ != a)

  /**
    * Kestrel combinator on the value of an option.
    * @param f the side-effecting function
    * @tparam B the result type
    * @return this option
    */
  @inline def tap[B](f: A => B): Option[A] = self <| { _ foreach f }

  /**
    *  An alias for tap.
    */
  @inline def <|?[B](f: A => B): Option[A] = tap(f)

  /**
    * A successful [[scala.util.Try]] of this option if present, or the given failure if empty.
    * @param failure the [[scala.util.Try]] failure if this option is empty
    * @return this option as a [[scala.util.Try]]
    */
  def toTry(failure: => Throwable): Try[A] =
    self.fold[Try[A]](Failure(failure))(Success(_))



  /**
    * Transforms `target` with the contained function if it exists,
    * otherwise, returns `target`.
    *
    * @param target the value to be potentially transformed
    * @return target transformed by the contained function, if it exists, otherwise target.
    */
  def transforming[T](target: T)(implicit ev: A <:< (T => T)): T =
    self.fold(target)(f => ev(f)(target))

  /**
    * An alias for `transforming`.
    */
  @inline def ~?>[T, U](target: T)(implicit ev: A <:< (T => T)): T =
    this transforming target

  /**
    * Flat to left disjunction. Turns this option into a left disjunction if present,
    * or else returns the supplied disjunction.
    * @param f the disjunction if this option is absent
    * @tparam AA the left type
    * @tparam B the right type
    * @return the resulting disjunction
    */
  @inline def <\/-[AA >: A, B](f: => A \/ B) =
    self.toLeftDisjunction(()).flatMap(_ => f)

  /**
    * Returns this, if present, or else optionally the supplied value if non-zero.
    * @param b the value
    * @tparam B the value type, with monoid evidence.
    * return this or else that
    */
  @inline def orNZ[B >: A : Monoid](b: => B): Option[B] = self orElse OptionOps.OptionNZ(b)

  /**
    * Filter the value to be non-zero.
    * @param ev monoid evidence for A
    * return this if non-zero
    */
  @inline def filterNZ(implicit ev: Monoid[A]): Option[A] = this - ev.zero

  /**
    * Runs the provided function as a side-effect if this is `None`, returns this option.
    * @param action the thing to do if this option is none
    * @return this option
    */
  def -<|[U](action: => U): self.type = {
    self ifNone { action ; () }
    self
  }


  /**
    * Put `self` on the left, and `right` on the right, of an Eitherneitherboth.
    *
    * @param right the option to put on the right
    * @return an Eitherneitherboth with `self` on the left and `right` on the right
    */
  @inline def \|/[B](right: Option[B]): A \|/ B =
   scaloi.\|/(self, right)


  /**
    * Append this optional value with another value in a semigroup.
    * @param b the other value
    * @tparam B the other type, with semigroup evidence
    * @return either the other value or the combined values
    */
  private[this] def append[B >: A : Semigroup](b: B): B = self.fold(b)(Semigroup[B].append(_, b))

  /**
    * Get the maximum of two optional values.
    * @param b the other optional value
    * @tparam B the other type
    * @return the max of the optional values
    */
  @inline def max[B >: A : Order](b: Option[B]): Option[B] = maxMonoid.append(self, b)


  /**
    * Get the maximum of this and a value.
    * @param b the other value
    * @tparam B the other type
    * @return the max of the values
    */
  @inline def max[B >: A : Order](b: B): B = append(b)(misc.Semigroups.maxSemigroup)

  /**
    * Get the minimum of two optional values.
    * @param b the other optional value
    * @tparam B the other type
    * @return the min of the optional values
    */
  @inline def min[B >: A : Order](b: Option[B]): Option[B] = minMonoid.append(self, b)

  /**
    * Get the minimum of this and a value.
    * @param b the other value
    * @tparam B the other type
    * @return the min of the values
    */
  @inline def min[B >: A : Order](b: B): B = append(b)(misc.Semigroups.minSemigroup)

  /**
    * Wrap the contained value in a `Gotten`, or create one with the
    * provided thunk and wrap it in a `Created.`
    * @param b the computation to create a value
    * @tparam B the type of the created value
    * @return the contained gotten value, or the supplied created value
    */
  @inline def orCreate[B >: A](b: => B): GetOrCreate[B] = self match {
    case Some(gotten) => GetOrCreate.gotten(gotten)
    case None         => GetOrCreate.created(b)
  }
}

/**
  * Option operations companion.
  */
object OptionOps extends ToOptionOps

/**
  * Implicit conversion for option operations.
  */
trait ToOptionOps extends Any {

  /**
    * Implicit conversion from option to the option enhancements.
    * @param o the optional thing
    * @tparam A its type
    */
  implicit def toOptionOps[A](o: Option[A]): OptionOps[A] = new OptionOps(o)

  /** Returns some if a value is non-null and non-zero, or else none.
    * @param a the value
    * @tparam A the value type with monoid evidence
    * @return the option
    */
  def OptionNZ[A : Monoid](a: A): Option[A] = Option(a).filterNZ

  /** Monoid evidence for the minimum over an option of an ordered type. */
  def minMonoid[A : Order]: Monoid[Option[A]] = optionMonoid(misc.Semigroups.minSemigroup)

  /** Monoid evidence for the maximum over an option of an ordered type. */
  def maxMonoid[A : Order]: Monoid[Option[A]] = optionMonoid(misc.Semigroups.maxSemigroup)
}