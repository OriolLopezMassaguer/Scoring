package scoring

import scopt._

case class ConfigScoring(
  command: String = "",
  findings_LOAEL_file: String = "",
  endpoints_file: String = "",
  output_file: String = "",
  units_factor_log10: Double = 6.0,
  debug: Boolean = false,
  pks_file: String = "")

object CommandLineParser {

  val parser = new scopt.OptionParser[ConfigScoring]("Scoring") {
    head("Scoring", "v1.4")

    cmd("endpoints") action { (_, c) =>
      c.copy(command = "computeendpoints")
    } text ("compute endpoints") children {

      opt[String]("findingsLOEL") action { (x, c) =>
        c.copy(findings_LOAEL_file = x)
      } text ("findings LOEL file is a string property")

      opt[String]("endpoints") action { (x, c) =>
        c.copy(endpoints_file = x)
      } text ("Endpoints file")

      opt[String]("output") action { (x, c) =>
        c.copy(output_file = x)
      } text ("Output file is a string property")

      opt[Unit]("debug") hidden () action { (x, c) =>
        c.copy(debug = true)
      } text ("Debug")

    }

    cmd("pktransform") action { (_, c) =>
      c.copy(command = "logarithmictransform")
    } text ("pk transform") children {

      opt[String]("endpointsLOEL") action { (x, c) =>
        c.copy(findings_LOAEL_file = x)
      } text ("Endpoints file is a string property")

      opt[String]("output") action { (x, c) =>
        c.copy(output_file = x)
      } text ("Output file is a string property")

      opt[Unit]("debug") hidden () action { (x, c) =>
        c.copy(debug = true)
      } text ("Debug")
    }

    cmd("chart") action { (_, c) =>
      c.copy(command = "chart")
    } text ("chart output") children {
      
      opt[String]("pks") action { (x, c) =>
        c.copy(pks_file = x)
      }text ("pks is a string property")
    }

  }
}