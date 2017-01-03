//package scoring
//
//import java.io.PrintStream
//import scala.Array.canBuildFrom
//import java.io.FileOutputStream
//import java.io.FileInputStream
//import java.io.ByteArrayOutputStream
//import scala.util.Random
//import scala.io._
//
//object DataTableBase {
//
//  type Row = Map[String, String]
//  case class RowGrouped(row: Row, dataTable: DataTableBase)
//  type GroupedDataTable = List[RowGrouped]
//
//  def apply(data: List[Row]): DataTableBase = new DataTableBase(data)
//
//  def apply(filename: String, separator: Char = '\t', fields: List[String] = List()): DataTableBase = {
//
//    def read_file_txt(filename: String, separator: Char, fieldsExt: List[String]) = {
//      //println("Source file")
//      try {
//        val sour = scala.io.Source.fromFile(filename)
//      } catch {
//        case e: Throwable => println(e)
//
//      }
//
//      val lines = scala.io.Source.fromFile(filename).getLines()
//      if (fieldsExt.isEmpty) {
//        val lines2 = scala.io.Source.fromFile(filename).getLines()
//        val fields = (lines2.next() + separator).split(separator).toList
//
//        var i = 0
//
//        val out = for (line <- lines.drop(1)) yield ({
//          i += 1
//
//          val values = (line + separator).split(separator)
//          val pairs = fields.zip(values)
//
//          val mp = pairs.toMap
//          mp.withDefaultValue("")
//        })
//
//        new DataTableBase(out.toList)
//
//      } else {
//        val linesList = lines.toList
//        val (fields, numDropLines) = if (fieldsExt.isEmpty) (linesList.head.split(separator).toList, 1) else (fieldsExt, 0)
//        var i = 0
//
//        val out = for (line <- linesList.drop(numDropLines)) yield ({
//          i += 1
//          val values = line.split(separator)
//          val pairs = fields.zip(values)
//          pairs.toMap
//        })
//        // println("Out: " + out)
//        new DataTableBase(out)
//      }
//    }
//    val l = filename.split('.')
//    val extension = l.last
//    extension match {
//
//      case _ => read_file_txt(filename, separator, fields)
//    }
//
//  }
//
//  def union_singleField(field: String, dts: List[DataTableBase]) = {
//    val values = dts.map(_.projectField(field)).flatten.toSet
//    val l = for (v <- values)
//      yield (Map(field -> v))
//
//    DataTableBase(l.toList)
//  }
//
//  def union(listDatatable: List[DataTableBase]): DataTableBase = {
//    listDatatable.foldRight(DataTableBase(List()))((dt1: DataTableBase, dt2: DataTableBase) => dt1.union(dt2))
//  }
//}
//
//class DataTableBase(data: List[DataTableBase.Row]) {
//
//  def log(s: String) = println(s)
//
//  val excluded_fields_export = Set("img_base64", "sdf", "SDF", "SDF2D")
//  def getData = data
//  private val first_fields = List("rownum", "database_substance_id", "smiles", "CANONICAL_SMILES", "min_dose", "max_dose", "count", "endpoint", "finding")
//  private val last_fields = List("Antagonists", "Agonists", "Unclassifiable")
//
//  def size = this.getRows.size
//
//  // Pet
//  //private val fields = (for (row <- data) yield (row.keySet)).toSet.flatten
//  private var fields = ((for (row <- data) yield (row.keySet)).toSet.flatten).toList
//
//  //private def getFields() = fields
//
//  private def setFields(fields: List[String]) = this.fields = fields
//
//  def sortFields(fields: List[String]) = {
//    this.setFields(fields)
//    this
//  }
//
//  def sortRows(fields: List[String]) = {
//    val dt = this.data
//    def fsort(fields: List[String], row: Map[String, String]): String = {
//      val fv = fields.map(field => row(field))
//      fv.mkString("|")
//    }
//    val d = dt.sortBy(row => fsort(fields, row))
//    DataTableBase(d)
//  }
//
//  // FPet
//
//  def getFields(fields_to_exclude: List[String] = List()) = {
//    val flds = fields.toList.toSet -- fields_to_exclude.toSet
//    var fieldsNew = flds -- first_fields.toSet -- last_fields
//    val fp = for (field_to_prioritize <- first_fields if fields.contains(field_to_prioritize))
//      yield (field_to_prioritize)
//    val fp2 = for (field_to_prioritize2 <- last_fields if fields.contains(field_to_prioritize2))
//      yield (field_to_prioritize2)
//    fp ++ fieldsNew.toList.sorted ++ fp2
//  }
//  val getRows = data.map(row => row.withDefaultValue(""))
//
//  def addRowNum = {
//    val rn = Range(0, this.size)
//    val dt = this.data.zip(rn)
//    val dt2 = for ((data, rnum) <- dt)
//      yield (data + ("rownum" -> rnum.toString))
//    DataTableBase(dt2)
//  }
//
//  // Transposing and pivoting
//  def transpose_simple(field: String, prefix: String = "", postfix: String = "") = {
//
//    val postfixN = if (postfix == "") "" else "_" ++ postfix
//
//    val prefixN = if (prefix == "") "" else prefix ++ "_"
//    val tr =
//      for (
//        row <- data;
//        (k, v) <- row if k == field
//      ) yield ({
//        prefixN ++ v ++ postfixN -> "1"
//      })
//    val mps = tr.toMap
//    mps.withDefaultValue("0")
//  }
//
//  def transpose(column: String, field_measure: String, prefix: String, postfix: String = "", defaultV: String) = {
//    val postfixN = if (postfix == "") "" else "_" ++ postfix
//    val prefixN = if (prefix == "") "" else prefix ++ "_"
//    val selected_fields =
//      for (row <- data)
//        yield (
//        for ((k, v) <- row if ((k == column) || k == field_measure))
//          yield ((k -> v)))
//
//    val mps = selected_fields
//    val mp = for (row <- selected_fields)
//      yield ({
//      val row2 = row.withDefaultValue(defaultV)
//      prefixN ++ row2(column) ++ postfixN -> row2(field_measure)
//    })
//    mp.toMap.withDefaultValue("")
//  }
//
//  def groupBy(fieldG: String) = {
//    val projectedFieldValues = projectField(fieldG)
//    for (value <- projectedFieldValues) yield ({
//      val restfields = for (row <- data if row(fieldG) == value) yield (row - fieldG)
//      ((fieldG -> value), DataTableBase(restfields))
//    })
//  }
//
//  def groupBy2(fields: List[String]) = {
//    val merged = this.merge_fields(fields, "#")
//    //println("Merged: " + merged.size)
//    val fieldc = fields.mkString("#")
//    //println(fieldc)
//    merged.groupBy(fieldc)
//  }
//
//  def groupByFields(fieldsGroup: List[String], fieldsProject: List[String] = List()): DataTableBase.GroupedDataTable = {
//
//    def gf(row: DataTableBase.Row): DataTableBase.Row = {
//      val fieldsRow = row.toMap
//      row -- (fieldsRow.keySet -- fieldsGroup)
//    }
//
//    val groupedDT = this.data.groupBy(gf)
//    val gr2 =
//      if (fieldsProject.isEmpty)
//        (groupedDT.map(r => DataTableBase.RowGrouped(r._1, DataTableBase(r._2).dropFields(fieldsGroup).unique)))
//      else
//        (groupedDT.map(r => DataTableBase.RowGrouped(r._1, DataTableBase(r._2).dropFields(fieldsGroup).project(fieldsProject).unique)))
//    gr2.toList
//  }
//
//  // Filtering
//
//  def filter(field: String, value: String) = {
//    val l = for (row <- this.data if row(field) == value) yield (row)
//    DataTableBase(l)
//  }
//  def filter(field: String, values: List[String]) = {
//    val l = for (row <- this.data if values.contains(row(field))) yield (row)
//    DataTableBase(l)
//  }
//  //Pet
//
//  def filterFieldNotNull(field: String) = this.filter((r: DataTableBase.Row) => r(field) != "")
//
//  def filter(rowFilter: DataTableBase.Row => Boolean) = DataTableBase(this.getRows.filter(rowFilter))
//  def product(otherDT: DataTableBase) = {
//    val tt = for (row1 <- this.getRows; row2 <- otherDT.getRows) yield (row1 ++ row2)
//    DataTableBase(tt)
//  }
//
//  //FPet
//  def pivot_simple(fieldGroup: String, field: String, prefix: String, postfix: String) = {
//    val grouped = groupBy(fieldGroup)
//    val transposed = grouped.map(a => (a._1, a._2.transpose_simple(field, prefix, postfix)))
//    val out = for (((k, v), rest) <- transposed)
//      yield ({
//      val pair = Map(k -> v)
//      pair ++ rest
//    })
//    val out2 = out.toList.map(_.withDefaultValue("0"))
//    val dt = DataTableBase(out2)
//    dt
//  }
//
//  def unpivot(fieldsGroup: List[String], value: String = "1", pivotField: String = "endpoint") = {
//    val r2 = for (row <- this.getRows)
//      yield ({
//      val mpGroup = row.filterKeys(fieldsGroup.contains(_))
//      val mpNoGroup = row.filterKeys(!fieldsGroup.contains(_))
//      val fieldsWithValue = for ((k, v) <- mpNoGroup if v == value) yield (k)
//      val result = for (field <- fieldsWithValue)
//        yield ({
//        val mp = mpGroup ++ Map(pivotField -> field)
//        mp
//      })
//      result
//    })
//    DataTableBase(r2.flatten)
//  }
//
//  def pivot_simple(fieldGroup: String, columns: List[String], prefix: String, postfix: String): DataTableBase = {
//    val grouped = groupBy(fieldGroup)
//    val dataTable_merged = merge_fields(columns)
//    val concatString = "_"
//    dataTable_merged.pivot_simple(fieldGroup, columns.mkString(concatString), prefix, postfix)
//  }
//
//  def pivotMeasure(fieldGroup: String, column: String, measure: String, prefix: String, postfix: String) = {
//    val grouped = groupBy(fieldGroup)
//    val transposed = grouped.map(a => (a._1, a._2.transpose(column, measure, prefix, postfix, "0.0")))
//    val out = for (((k, v), rest) <- transposed)
//      yield ({
//      val pair = Map(k -> v)
//      pair ++ rest
//    })
//    val out2 = out.toList.map(_.withDefaultValue("0.0"))
//    val dt = DataTableBase(out2)
//    dt
//  }
//
//  def pivotMeasure(fieldGroup: String, columns: List[String], measure: String, prefix: String, postfix: String): DataTableBase = {
//    val dataTable_merged = this.merge_fields(columns)
//    val fields_merged = columns.mkString("_")
//    val dataTablePivoted = dataTable_merged.pivotMeasure(fieldGroup, fields_merged, measure, prefix, postfix)
//    dataTablePivoted
//  }
//
//  def merge_fields(fields: List[String], concatString: String = "_"): DataTableBase = {
//    val res = for (row <- this.getRows)
//      yield ({
//      val values_fields = fields.map(field => row(field))
//      val new_row = row
//      val fieldConcat = fields.mkString(concatString)
//      val valueConcat = values_fields.mkString(concatString)
//      val nr = Map(fieldConcat -> valueConcat)
//      new_row ++ nr
//    })
//    DataTableBase(res)
//  }
//
//  // Adding/Renaming Fields
//  def addConstantField(field: String, value: String) = {
//    val l = for (row <- this.getRows)
//      yield (row + (field -> value))
//    new DataTableBase(l)
//  }
//
//  def addFields(fields: List[(String, String, String => String)]) =
//    {
//      val s = for (row <- this.getRows)
//        yield ({
//
//        val pairs = for ((field_base, field_new, f) <- fields)
//          yield ({
//          //println(row)
//          try { f(row(field_base)) }
//          catch {
//            case e: Throwable => ""
//          }
//
//          val pair = (field_new, f(row(field_base)))
//          pair
//        })
//        val newrow = row ++ pairs
//        newrow
//      })
//      new DataTableBase(s)
//    }
//
//  def addField(field_base: String, field_new: String, f: String => String) = addFields(List((field_base, field_new, f)))
//
//  private val guess_smilesField = {
//    val fields = this.getFields()
//    val fieldsUpper = fields.map(f => (f, f.toUpperCase()))
//    val fieldCandidates = fieldsUpper.filter(p => p._2.contains("SMILES"))
//    val firstCandidate =
//      if (fieldCandidates.size == 0)
//        ""
//      else
//        fieldCandidates.head._1
//    firstCandidate
//  }
//
//  def renameFields(fieldMapping: Map[String, String]) = {
//    val fieldsToMap = fieldMapping.keySet
//    val rows = for (row <- this.getRows)
//      yield ({
//      val new_row = for ((k, v) <- row)
//        yield ({
//        if (fieldsToMap.contains(k))
//          fieldMapping(k) -> v
//        else
//          k -> v
//      })
//      new_row
//    })
//    DataTableBase(rows)
//  }
//  // Relational operators
//
//  // Projection
//
//  // Ini Pet
//  def getMap(domain: String, range: String) = {
//    val projected = this.project(List(domain, range)).unique
//    val mp = for (r <- this.getData)
//      yield ({
//      r(domain) -> r(range)
//    })
//    mp.toMap
//  }
//  //End Pet
//
//  def projectField(field: String) = (for (row <- data) yield (row(field))).toSet
//  def projectField_list(field: String) = (for (row <- data) yield (row(field))).toList
//
//  def project(fields: List[String]) = {
//    def filterRow(row: DataTableBase.Row) = row.filter(pair => fields.contains(pair._1))
//    val projected = this.data.map(filterRow(_))
//    DataTableBase(projected)
//  }
//
//  def dropFields(fields: List[String]) = {
//    def filterRow(row: DataTableBase.Row) = row.filter(pair => !fields.contains(pair._1))
//    val projected = this.data.map(filterRow(_))
//    DataTableBase(projected)
//  }
//  def dropStructures(drop: Boolean) =
//    {
//      if (drop)
//        this.dropFields(List("smiles"))
//      else
//        this
//    }
//
//  def unique = DataTableBase(this.data.distinct)
//  def take(i: Int) = DataTableBase(this.data.take(i))
//  // Joins
//
//  def join(otherDT: DataTableBase, fieldLeft: String, fieldRight: String): DataTableBase = {
//    //TODO existen campos 
//    //TODO campos coincidentes?
//    val out =
//      for (
//        rowLeft <- this.getData;
//        rowRight <- otherDT.getData if (rowLeft(fieldLeft) == rowRight(fieldRight))
//      ) yield (rowLeft ++ (rowRight - fieldRight))
//    val out2 = out.toList.map(_.withDefaultValue(""))
//    DataTableBase(out2)
//  }
//
//  def join(otherDT: DataTableBase, fieldsLeft: List[String], fieldsRight: List[String]): DataTableBase = {
//    val merged_fields_left = fieldsLeft.mkString("_")
//    val merged_fields_right = fieldsRight.mkString("_")
//    val dt1 = this.merge_fields(fieldsLeft, "_")
//    val dt2 = otherDT.merge_fields(fieldsRight, "_")
//    dt1.join(dt2, merged_fields_left, merged_fields_right)
//  }
//
//  def union(otherDT: DataTableBase): DataTableBase = {
//    val rows1 = this.getRows
//    val rows2 = otherDT.getRows
//    DataTableBase(rows1 ++ rows2)
//  }
//
//  def union_tagged(otherDT: DataTableBase, tag1: String, tag2: String) = {
//    val rows1 = this.getRows.map(r => r + ("group" -> tag1))
//    val rows2 = otherDT.getRows.map(r => r + ("group" -> tag2))
//    DataTableBase(rows1 ++ rows2)
//  }
//
//  def join_left_all(otherDT: DataTableBase, fieldLeft: String, fieldRight: String) = {
//    val blankrowRight = (otherDT.getFields().toSet - fieldRight).toList.map(v => (v, ""))
//    val valuesLeft = this.projectField(fieldLeft)
//    val valuesRight = otherDT.projectField(fieldRight)
//    val valsToAdd = valuesLeft -- valuesRight
//    val outerRows = for (rowLeft <- this.getData if valsToAdd.contains(rowLeft(fieldLeft))) yield (rowLeft ++ blankrowRight.toMap)
//
//    val equijoin = this.join(otherDT, fieldLeft, fieldRight)
//    log("Left join")
//    log("Left records: " + this.getRows.size)
//    log("Right records: " + otherDT.getRows.size)
//    log("Join records: " + equijoin.getRows.size)
//    log("Outer records: " + outerRows.size)
//    (DataTableBase(equijoin.getRows ++ outerRows), DataTableBase(outerRows))
//  }
//
//  def join_left(otherDT: DataTableBase, fieldLeft: String, fieldRight: String) = join_left_all(otherDT, fieldLeft, fieldRight)._1
//
//  //def rightjoin(otherDT: DataTableBase) = { }
//
//  //def fulloutertjoin(otherDT: DataTableBase) = { }
//
//  def joinfunctional(fieldLeft: String, function: String => DataTableBase): DataTableBase = {
//    val result = for (row <- this.getRows) yield ({
//      println("Join: " + row)
//      val dtJoined = function(row(fieldLeft))
//      val newRows = for (rowDerived <- dtJoined.getRows) yield ({
//        println("Join Derived: " + rowDerived)
//        row ++ rowDerived
//      })
//      newRows
//    })
//    DataTableBase(result.flatten)
//  }
//
//  //BuildingMaps
//  def buildMap(domainField: String, rangeField: String) = {
//    val mp = for (row <- this.data)
//      yield ({
//      row(domainField) -> row(rangeField)
//    })
//    mp.toMap
//  }
//
//  def sortBy(field: String) = DataTableBase(this.data.sortBy(mp => mp(field)))
//
//  def toTextDV(filename: String, defaultValue: String): Unit = {
//    val fos = new PrintStream(filename)
//    this.toText(fos, defaultValue = defaultValue)
//    fos.close
//  }
//
//  def toText(filename: String): Unit = {
//    val fos = new PrintStream(filename)
//    this.toText(fos)
//    fos.close
//  }
//
//  def toTextAllfields(filename: String): Unit = {
//    val fos = new PrintStream(filename)
//    this.toText(fos, Set("sdf"))
//    fos.close
//  }
//
//  lazy val toTextTest: String = {
//    val baos = new ByteArrayOutputStream()
//    val ps = new PrintStream(baos)
//    toText(ps)
//    val content = baos.toString()
//    content
//  }
//
//  def toText(out: PrintStream, fields_to_exclude: Set[String] = excluded_fields_export, separator: Char = '\t', quoted: Boolean = false, defaultValue: String = ""): Unit = {
//
//    val fields = this.getFields()
//    for ((column, i) <- fields.toList zip (0 to (fields.size - 1))) {
//      if (!(fields_to_exclude contains column)) {
//        out.print(column)
//        if (i < fields.size - 1) out.print(separator)
//      }
//    }
//    out.println()
//    for (row <- this.getRows.toIterator) {
//      for ((column, i) <- fields.toList zip (0 to (fields.size - 1))) {
//        if (!(fields_to_exclude contains column)) {
//          if (quoted)
//            out.print('"' + row.withDefaultValue(defaultValue)(column) + '"')
//          else
//            out.print(row.withDefaultValue(defaultValue)(column))
//          if (i < fields.size - 1) out.print(separator)
//        }
//      }
//      out.println()
//    }
//  }
//
//  override def toString = {
//    val fieldsSorted = this.getFields().sorted
//    val fields = fieldsSorted.mkString("|") + "\n"
//    val rowss = for (row <- this.getRows) yield (fieldsSorted.map(row(_)).mkString("|"))
//    fields + rowss.mkString("\n")
//  }
//  def toMdown = {
//    val fieldsSorted = this.getFields().sorted
//    val fields = fieldsSorted.mkString("|") + "\n"
//    val fieldsSep = fieldsSorted.map((x: String) => " ").mkString("|") + "\n"
//    val rowss = for (row <- this.getRows) yield (fieldsSorted.map(row(_)).mkString("|"))
//    fields + fieldsSep + rowss.mkString("\n")
//  }
//}
//
//
//
