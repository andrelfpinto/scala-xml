package scala.xml

import org.junit.Test

class CompilerErrors extends CompilerTesting {
  @Test
  def t7185() =
    expectXmlError("""|overloaded method value apply with alternatives:
                      |  (f: scala.xml.Node => Boolean)scala.xml.NodeSeq <and>
                      |  (i: Int)scala.xml.Node
                      | cannot be applied to ()""".stripMargin,
     """|object Test {
        |  <e></e>()
        |}""")

  @Test
  def t1878_typer() =
    expectXmlError("_* may only come last",
     """|object Test extends App {
        |  // illegal - bug #1764
        |  null match {
        |    case <p> { _* } </p> =>
        |  }
        |}""")


  @Test
  def t1017() =
    expectXmlErrors(1, "not found: value foo",
     """|object Test {
        |  <x><x><x><x><x><x><x><x><x><x><x><x><x><x><x><x><x><x>{ foo }</x></x></x></x></x></x></x></x></x></x></x></x></x></x></x></x></x></x>
        |}""")

  @Test
  def t1011() =
    expectXmlErrors(69, "not found: value entity",
     """|import scala.xml._;
        |
        |abstract class Test {
        |  //val entity : String;
        |  def primitiveHeader : NodeSeq =
        |    Group({
        |       <dl><code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code>
        |           <code>{Text(entity)}</code></dl>
        |    } ++ // 3 seconds
        |    {}++ // 5 seconds
        |    {}++ // 10 seconds
        |    {}++ // 20 seconds
        |    {}++ // 40 seconds
        |    {}++ // 40 seconds
        |    {}++ // 40 seconds
        |    {}++ // 40 seconds
        |    {}++ // 40 seconds
        |    {}++ // 40 seconds
        |    {}++ // 40 seconds
        |    {}++ // 40 seconds
        |    {}++ // 40 seconds
        |    {}++ // 40 seconds
        |    {}++ // 40 seconds
        |    {}++ // 40 seconds
        |    {}++ // 40 seconds
        |    {}++ // 5 seconds
        |    {}++ // 10 seconds
        |    {}++ // 20 seconds
        |    {}++ // 40 seconds
        |    {}++ // 40 seconds
        |    {}++ // 40 seconds
        |    {}++ // 40 seconds
        |    {}++ // 40 seconds
        |    {}++ // 40 seconds
        |    {}++ // 40 seconds
        |    {}++ // 40 seconds
        |    {}++ // 40 seconds
        |    {}++ // 40 seconds
        |    {}++ // 40 seconds
        |    {}++ // 40 seconds
        |    {}++ // 40 seconds
        |    {}++ // 5 seconds
        |    {}++ // 10 seconds
        |    {}++ // 20 seconds
        |    {}++ // 40 seconds
        |    {}++ // 40 seconds
        |    {}++ // 40 seconds
        |    {}++ // 40 seconds
        |    {}++ // 40 seconds
        |    {}++ // 40 seconds
        |    {}++ // 40 seconds
        |    {}++ // 40 seconds
        |    {}++ // 40 seconds
        |    {}++ // 40 seconds
        |    {}++ // 40 seconds
        |    {}++ // 40 seconds
        |    {}++ // 40 seconds
        |    <hr/>);
        |}""")
}

// TODO: factor out somewhere?
class CompilerTesting {
  lazy val printVerOnce = println(s"Testing scala-xml version ${Properties.versionNumberString}")

  def errorMessages(errorSnippet: String, compileOptions: String = "")(code: String): List[String] = {
    debugClassloaders(getClass.getClassLoader)
    printVerOnce
    import scala.tools.reflect._
    val m  = scala.reflect.runtime.currentMirror
    val tb = m.mkToolBox(options = compileOptions) //: ToolBox[m.universe.type]
    val fe = tb.frontEnd

    try {
      tb.eval(tb.parse(code))
      Nil
    } catch { case _: ToolBoxError =>
      import fe._
      infos.toList collect { case Info(_, msg, ERROR) => msg }
    }
  }

  def debugClassloaders(cl: ClassLoader) = {
    def debugLoader(cl: ClassLoader): Unit = {
      cl match {
        case url: java.net.URLClassLoader =>
          println(s"* URLCLassloader {\n   * ${url.getURLs.map(_.toString).mkString("\n   * ")}\n  })")
        case other =>
          println(s"* ${other.getClass.getName}")
      }
      Option(cl.getParent) match {
        case None => ()
        case Some(cl) => debugLoader(cl)
      }
    }  
    println("-= Classloader hierarchy =-")
    debugLoader(cl)
  }

  // note: `code` should have a | margin
  // the import's needed because toolbox compiler does not accumulate imports like the real one (TODO: verify hypothesis)
  def xmlErrorMessages(msg: String, code: String) =
    errorMessages(msg)("import scala.xml.{TopScope => $scope}\n"+ code.stripMargin)

  def expectXmlError(msg: String, code: String) = {
    val errors = xmlErrorMessages(msg, code)
    assert(errors exists (_ contains msg), errors mkString "\n")
  }

  def expectXmlErrors(msgCount: Int, msg: String, code: String) = {
    val errors = xmlErrorMessages(msg, code)
    val errorCount = errors.filter(_ contains msg).length
    assert(errorCount == msgCount, s"$errorCount occurrences of \'$msg\' found -- expected $msgCount in:\n${errors mkString "\n"}")
  }
}