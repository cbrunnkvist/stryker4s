package stryker4s.mutants.applymutants

import grizzled.slf4j.Logging
import stryker4s.extension.TreeExtensions.{IsEqualExtension, TransformOnceExtension}
import stryker4s.extension.exception.UnableToBuildPatternMatchException
import stryker4s.model.{Mutant, SourceTransformations, TransformedMutants}
import stryker4s.mutants.applymutants.ActiveMutationContext.ActiveMutationContext

import scala.meta._
import scala.util.{Failure, Success}

class MatchBuilder(mutationContext: ActiveMutationContext) extends Logging {
  def buildNewSource(transformedStatements: SourceTransformations): Tree = {
    val source = transformedStatements.source

    groupTransformedStatements(transformedStatements).foldLeft(source: Tree) { (rest, mutants) =>
      val origStatement = mutants.originalStatement

      rest transformOnce {
        case found if found.isEqual(origStatement) && found.pos == origStatement.pos =>
          buildMatch(mutants)
      } match {
        case Success(value) => value
        case Failure(exception) =>
          error(s"Failed to construct pattern match: original statement [$origStatement]")
          error(s"Failed mutation(s) ${mutants.mutantStatements.mkString(",")}.")
          error(
            s"at ${origStatement.pos.input}:${origStatement.pos.startLine + 1}:${origStatement.pos.startColumn + 1}"
          )
          error("This is likely an issue on Stryker4s's end, please enable debug logging and restart Stryker4s.")
          debug("Please open an issue on github: https://github.com/stryker-mutator/stryker4s/issues/new")
          debug("Please be so kind to copy the stacktrace into the issue", exception)

          throw UnableToBuildPatternMatchException()
      }
    }
  }

  def buildMatch(transformedMutant: TransformedMutants): Term.Match = {
    val cases: List[Case] = transformedMutant.mutantStatements.map(mutantToCase) :+ defaultCase(
      transformedMutant.originalStatement
    )

    val activeMutationEnv = Lit.String("ACTIVE_MUTATION")
    // q"(_root_.scala.sys.$mutationContext.get($activeMutationEnv) match { ..case $cases })"
    Term.Match(
      Term.Apply(
        Term.Select(
          Term.Select(
            Term.Select(
              Term.Select(
                Term.Name("_root_"),
                Term.Name("scala")
              ),
              Term.Name("sys")
            ),
            mutationContext
          ),
          Term.Name("get")
        ),
        List(activeMutationEnv)
      ),
      cases
    )
  }

  private def mutantToCase(mutant: Mutant): Case =
    buildCase(mutant.mutated, Pat.Extract(Term.Name("Some"), List(Lit.String(mutant.id.toString()))))

  private def defaultCase(originalStatement: Term): Case = buildCase(originalStatement, Pat.Wildcard())

  private def buildCase(expression: Term, pattern: Pat): Case = Case(pattern, None, expression)

  private def groupTransformedStatements(transformedStatements: SourceTransformations): Seq[TransformedMutants] = {
    transformedStatements.transformedStatements
      .groupBy(_.originalStatement)
      .map({
        case (key, transformedMutants) =>
          (key, transformedMutants.flatMap(transformedMutant => transformedMutant.mutantStatements))
      })
      .map({ case (originalStatement, mutants) => TransformedMutants(originalStatement, mutants.toList) })
      .toSeq
      .sortBy(_.mutantStatements.head.id) // Should be sorted so tree transformations are applied in order of discovery
  }
}
