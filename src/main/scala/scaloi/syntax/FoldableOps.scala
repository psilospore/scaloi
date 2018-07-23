package scaloi
package syntax

import scala.language.implicitConversions
import scalaz.{Foldable, Monoid, Unapply}

/**
  * Enhancements on foldable things.
  * @param self the foldable thing
  * @tparam F the foldable type
  * @tparam A the folded type
  */
final class FoldableOps[F[_], A](val self: F[A]) extends AnyVal {
  /**
    * Apply a map to an optional value to the elements of this foldable, returning
    * the first defined result.
    *
    * @param f the map function
    * @param ev foldable evidence
    * @tparam B the target type
    * @return the optional value
    */
  @inline final def findMap[B](f: A => Option[B])(implicit ev: Foldable[F]): Option[B] =
    ev.findMapM[scalaz.Id.Id, A, B](self)(f)

  /** Fold a foldable's worth of elements into another foldable's worth of
    * monoidal values, then combine those values monoidally.
    */
  /* the desired implementation is
   * @inline final def flatFoldMap[G[_], B](f: A => G[B])(
   *   implicit F: Foldable[F], G: Foldable[G], B: Monoid[B]
   * ): B = {
   *   F.foldMap(self)(a => G.fold(f(a)))
   * }
   * however, intellij is still plagued by the classic partial unification bug
   */
  @inline final def flatFoldMap[GB, B](f: A => GB)(
    implicit F: Foldable[F], GB: Unapply.AuxA[Foldable, GB, B], B: Monoid[B]
  ): B = {
    F.foldMap(self)(a => GB.TC.fold[B](GB.leibniz(f(a)))(B))
  }
}

/**
  * Foldable operations companion.
  */
object FoldableOps extends ToFoldableOps

/**
  * Implicit conversion for foldable operations.
  */
trait ToFoldableOps {

  /**
    * Implicit conversion from foldable to its enhancements.
    * @param f: the foldable thing
    * @tparam F the foldable type
    * @tparam A the folded type
    */
  implicit def toFoldableOps[F[_] : Foldable, A](f: F[A]): FoldableOps[F, A] =
    new FoldableOps(f)
}
