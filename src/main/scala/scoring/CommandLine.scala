package scoring

object CommandLine {

  def main(args: Array[String]): Unit = {

    val cfg = CommandLineParser.parser.parse(args, ConfigScoring())

    cfg match {
      case Some(cf) =>
        {
          cf.command match {

            case "computeendpoints" => {

              Scoring.computePatterns(
                cf.findings_LOAEL_file,
                cf.endpoints_file,
                cf.output_file,
                !cf.debug)
            }
            case "logarithmictransform" => {
              Scoring.transformLogaritmicScale(
                cf.findings_LOAEL_file,
                cf.output_file,
                cf.units_factor_log10,
                !cf.debug)
            }
            case "chart" => {
              Scoring.chart(cf.pks_file)
            }
            case _ => CommandLineParser.parser.showUsage
          }
        }
      case None => {
        import scala.collection.JavaConversions._
        CommandLineParser.parser.showUsage
        System.getenv.toList.sortBy(_._1).map(println)
        val mp=System.getenv
        println(mp.size())
        
      }
    }

  }

}