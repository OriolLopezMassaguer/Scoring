package scoring

import scala.collection.JavaConversions._
import models.dataframe._
import java.io.PrintStream

object Scoring_prepare {
  val dt = DataFrame("data/Patterns/input/Liver_HPF_Patterns_Oriol.txt")
  val dt2 = scoring.Scoring_prepare.dt.pivot_simple("finding", List("endpoint"), "", "").sortBy("finding")
  val fos = new PrintStream("data/Patterns/output/patterns_pivoted.txt")
  dt2.toText(fos, defaultValue = "0")
  val dt3 = dt2.unpivot(List("finding"))
  val fos2 = new PrintStream("data/Patterns/output/patterns_unpivoted.txt")
  dt3.toText(fos2, defaultValue = "0")
  val fos3 = new PrintStream("data/Patterns/output/patterns_pivoted_re.txt")
  dt3.pivot_simple("finding", List("endpoint"), "", "").sortBy("finding").toText(fos3, defaultValue = "0")
  Scoring.computePatterns("data/Patterns/output/0_HistopathologicalFinding_liver_TR_LOAEL.tsv", "data/Patterns/output/patterns_pivoted.txt", "data/Patterns/output/findings_with_pattern_v2.tsv")
}

object Scoring {

  val port_js_server = unfiltered.util.Port.any

  val preservedFields = List("pharmacological_action", "database_substance_id", "smiles", "min_dose", "max_dose")

  private def getPatterns(file: String): Map[String, List[String]] = {
    val patternsDT = DataFrame(file)
    getPatterns(patternsDT)
  }

  private def getPatterns(patterns: DataFrame): Map[String, List[String]] = {
    val patternsDT = patterns
    val patternsMap = {
      val l = for (pattern <- patternsDT.projectField("endpoint")) yield ({
        val findings = patternsDT.filter("endpoint", pattern).projectField("finding")
        pattern -> findings.toList
      })

      l.toMap
    }
    patternsMap
  }

  def computePatterns(finding_LOEL: DataFrame, patternsInput: DataFrame, dropInputFields: Boolean): DataFrame = {

    val patterns = patternsInput.unpivot(List("finding"))
    val findings_in_patterns = this.getPatterns(patterns).values.flatten.toList

    def searchFindings(findings_in_patterns: List[String], findings_input_LOAEL: DataFrame) = {
      val fields_Findings_LOAEL = findings_input_LOAEL.getFields()
      def findField(field_finding: String, finding_pattern: String) = {
        val fragments = field_finding.split("_")
        fragments.contains(finding_pattern)
      }

      val lFieldsFound = for (finding_pattern <- findings_in_patterns) yield ({
        val fields_found = fields_Findings_LOAEL.filter(findField(_, finding_pattern))
        (finding_pattern, fields_found)
      })
      (lFieldsFound.filter(_._2.size == 0).map(_._1), (lFieldsFound.filter(_._2.size != 0).map(t => (t._1, t._2.head))).toMap)
    }

    val (_, fields_mapping) = searchFindings(findings_in_patterns, finding_LOEL)

    val patterns_Filtered = {
      val fieldsPattern = fields_mapping.keys.toList
      val patternsFiltered = patterns.filter("finding", fieldsPattern)
      val rows = for (row <- patternsFiltered.getRows) yield (row + ("finding2" -> fields_mapping(row("finding"))))
      DataFrame(rows).dropFields(List("finding")).renameFields(Map("finding2" -> "finding"))
    }

    val patterns_Mapping = this.getPatterns(patterns_Filtered)
    val fields_findings = patterns_Mapping.values.flatten.toList
    val fields_patterns = patterns_Mapping.keys.toList

    val computePatterns = {
      val rows = for (row <- finding_LOEL.getRows)
        yield ({
        val patternsComp = for ((pattern, fields) <- patterns_Mapping)
          yield ({
          val values = fields.map(row(_)).filter(s => s != "").map(_.toFloat)
          val pvalue = if (values.size == 0) "" else values.min.toString
          pattern -> pvalue
        })
        row ++ patternsComp
      })
      DataFrame(rows)
    }
    //Debug    

    val computePatternsFinal = if (dropInputFields) {
      computePatterns.project((fields_patterns ++ preservedFields):_*)
    } else {
      patterns_Filtered.toText("filtered_and_mapped_patterns.tsv")
      computePatterns
    }

    computePatternsFinal

  }

  def computePatterns(finding_file_LOAEL: String, patterns_file: String, output_file: String, dropInputFields: Boolean = true): Unit = {
    lazy val patterns = Scoring.getPatterns(patterns_file)
    val patternsDT = DataFrame(patterns_file)
    lazy val findings_input_LOAEL = DataFrame(finding_file_LOAEL)
    val output = computePatterns(findings_input_LOAEL, patternsDT, dropInputFields)
    output.toText(output_file)
  }

  def transformLogaritmicScale(finding_LOAEL: DataFrame, unitsScale: Double, dropInputFields: Boolean, fields: List[String]): DataFrame =
    {
      val fieldsToTransform = fields match {
        case List() => finding_LOAEL.getFields().filter(field => !preservedFields.contains(field))
        case fields => fields
      }

      def parseDouble(s: String) = try { Some(s.toDouble) } catch { case _: Throwable => None }

      val rows = for (row <- finding_LOAEL.getRows)
        yield ({
        val new_row = for (field <- fieldsToTransform)
          yield ({
          (field + "_score") -> {
            val a = (parseDouble(row(field)) match {
              case None        => if (row(field) == "") "0" else row(field)
              case Some(0.0)   => "0"
              case Some(value) => (unitsScale - scala.math.log10(value)).toString
            })
            a
          }

        })
        row ++ new_row.toMap
      })
      val newFields = for (field <- fieldsToTransform) yield (field + "_score")
      val result = DataFrame(rows)

      val resultFinal = if (dropInputFields)
        result.project((preservedFields ++ newFields):_*)
      else
        result
      resultFinal

    }

  def transformLogaritmicScale(finding_file_LOAEL: String, finding_file_Logaritmic: String, unitsScale: Double, dropInputFields: Boolean = true, fields: List[String] = List()): Unit = {
    val dt = DataFrame(finding_file_LOAEL)
    val out = transformLogaritmicScale(dt, unitsScale, dropInputFields, fields)
    showStatistics(out)
    out.toText(finding_file_Logaritmic)
  }

  def showStatistics(data: DataFrame) = {
    val fields_to_compute = data.getFields(List()).toSet -- Set("database_substance_id", "smiles", "min_dose", "max_dose").toList.sorted
    def round(x: Double, scale: Int = 2) = BigDecimal(x).setScale(scale, BigDecimal.RoundingMode.HALF_UP).toDouble

    def showStatisticsForField(field: String) = {
      val values = data.projectField_list(field).map(_.toDouble)
      val positives = values.filter(_ > 0.0)
      val res @ (min, max, positivescnt, positivepct, average) = (round(positives.min), round(positives.max), positives.size, round(100.0 * (positives.size.toDouble / values.size.toDouble), 3), round(positives.sum / data.size))

      println(s"$field: $positivescnt positive ($positivepct%), $min min, $max max, average $average")
    }
    val size = data.size
    println(s"Processed $size compounds")
    println
    fields_to_compute.map(showStatisticsForField(_))
  }

  def chart(pks_file: String): Unit = {
    val dt = DataFrame(pks_file)
    chart(dt)
  }

  def chart(pks: DataFrame): Unit = {
    import com.quantifind.charts.Highcharts._

    def server() = {      
      val homepath = System.getenv("SCRIPTPATH")
      val r = new java.io.File(homepath).toURI().toURL()

      println(r)
      val server2 = unfiltered.jetty.Server
        .http(Scoring.port_js_server)
        .resources(r)
      server2.start()
    }

    def chart_variable(variable: String) = {
      val values = pks.projectField_list(variable).map(_.toDouble)
      val (negatives, positives) = (values.filter(_ == 0).size, values.filter(_ != 0).size)
      val h = histogram_custom(values.filter(_ != 0), 10, 1.0, 9.0)
      title(variable)
      val p = pie(List(negatives, positives))
      title(variable)
    }
    server()
    //println(pks.getFields(List("database_substance_id", "smiles", "min_dose", "max_dose")))
    for (variable <- (pks.getFields(List()).toSet -- Set("database_substance_id", "smiles", "min_dose", "max_dose")).toList.sorted.reverse) {
      chart_variable(variable)
    }

  }

}


object TestScoring {

  //val dt = DataFrame("data/0_HistopathologicalFinding_liver_TR_LOEL_endpoints_pk_rat.tsv")
  //def ch = Scoring.chart(dt)

}

