/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2003-2009, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

// $Id$


package scala.xml

import collection.mutable.{Set, HashSet}
import parsing.XhtmlEntities

/**
 * The <code>Utility</code> object provides utility functions for processing
 * instances of bound and not bound XML classes, as well as escaping text nodes.
 *
 * @author Burak Emir
 */
object Utility extends AnyRef with parsing.TokenTests
{
  implicit def implicitSbToString(sb: StringBuilder) = sb.toString()

  // helper for the extremely oft-repeated sequence of creating a
  // StringBuilder, passing it around, and then grabbing its String.
  private [xml] def sbToString(f: (StringBuilder) => Unit): String = {
    val sb = new StringBuilder
    f(sb)
    sb.toString
  }
  private[xml] def isAtomAndNotText(x: Node) = x.isAtom && !x.isInstanceOf[Text]

  /** trims an element - call this method, when you know that it is an
   *  element (and not a text node) so you know that it will not be trimmed
   *  away. With this assumption, the function can return a <code>Node</code>,
   *  rather than a <code>Seq[Node]</code>. If you don't know, call
   *  <code>trimProper</code> and account for the fact that you may get back
   *  an empty sequence of nodes.
   *
   *  precondition: node is not a text node (it might be trimmed)
   */
  def trim(x: Node): Node = x match {
    case Elem(pre, lab, md, scp, child@_*) =>
      Elem(pre, lab, md, scp, (child flatMap trimProper):_*)
  }

  /** trim a child of an element. <code>Attribute</code> values and
   *  <code>Atom</code> nodes that are not <code>Text</code> nodes are unaffected.
   */
  def trimProper(x:Node): Seq[Node] = x match {
    case Elem(pre,lab,md,scp,child@_*) =>
      Elem(pre,lab,md,scp, (child flatMap trimProper):_*)
    case Text(s) =>
      new TextBuffer().append(s).toText
    case _ =>
      x
  }
  /** returns a sorted attribute list */
  def sort(md: MetaData): MetaData = if((md eq Null) || (md.next eq Null)) md else {
    val key = md.key
    val smaller = sort(md.filter { m => m.key < key })
    val greater = sort(md.filter { m => m.key > key })
    smaller.append( Null ).append(md.copy ( greater ))
  }

  /** returns the node with its attribute list sorted alphabetically (prefixes are ignored) */
  def sort(n:Node): Node = n match {
	  case Elem(pre,lab,md,scp,child@_*) =>
		  Elem(pre,lab,sort(md),scp, (child map sort):_*)
	  case _ => n
  }

  @deprecated("a string might also be Atom(s) - define your own conversion")
  def view(s: String): Text = Text(s)

  /**
   * Escapes the characters &lt; &gt; &amp; and &quot; from string.
   *
   * @param text ...
   * @return     ...
   */
  final def escape(text: String): String = sbToString(escape(text, _))

  object Escapes {
    /** For reasons unclear escape and unescape are a long ways from
        being logical inverses. */
    private val pairs = List(
      "lt"    -> '<',
      "gt"    -> '>',
      "amp"   -> '&',
      "quot"  -> '"'
      // enigmatic comment explaining why this isn't escaped --
      // is valid xhtml but not html, and IE doesn't know it, says jweb
      // "apos"  -> '\''
    )
    val escMap    = Map((for ((s, c) <- pairs) yield (c, "&%s;" format s)) : _*)
    val unescMap  = Map(("apos"  -> '\'') :: pairs : _*)
  }
  import Escapes._

  /**
   * Appends escaped string to <code>s</code>.
   *
   * @param text ...
   * @param s    ...
   * @return     ...
   */
  final def escape(text: String, s: StringBuilder): StringBuilder =
    text.foldLeft(s)((s, c) => escMap.get(c) match {
      case Some(str)  => s append str
      case None       => s append c
    })

  /**
   * Appends unescaped string to <code>s</code>, amp becomes &amp;
   * lt becomes &lt; etc..
   *
   * @param ref ...
   * @param s   ...
   * @return    <code>null</code> if <code>ref</code> was not a predefined
   *            entity.
   */
  final def unescape(ref: String, s: StringBuilder): StringBuilder =
    (unescMap get ref) map (s append _) getOrElse null

  /**
   * Returns a set of all namespaces used in a sequence of nodes
   * and all their descendants, including the empty namespaces.
   *
   * @param nodes ...
   * @return      ...
   */
  def collectNamespaces(nodes: Seq[Node]): Set[String] =
    nodes.foldLeft(new HashSet[String]) { (set, x) => collectNamespaces(x, set) ; set }

  /**
   * Adds all namespaces in node to set.
   *
   * @param n   ...
   * @param set ...
   */
  def collectNamespaces(n: Node, set: Set[String]) {
    if (n.doCollectNamespaces) {
      set += n.namespace
      for (a <- n.attributes) a match {
        case _:PrefixedAttribute =>
          set += a.getNamespace(n)
        case _ =>
      }
      for (i <- n.child)
        collectNamespaces(i, set)
    }
  }

  // def toXML(
  //   x: Node,
  //   pscope: NamespaceBinding = TopScope,
  //   sb: StringBuilder = new StringBuilder,
  //   stripComments: Boolean = false,
  //   decodeEntities: Boolean = true,
  //   preserveWhitespace: Boolean = false,
  //   minimizeTags: Boolean = false): String =
  // {
  //   toXMLsb(x, pscope, sb, stripComments, decodeEntities, preserveWhitespace, minimizeTags)
  //   sb.toString()
  // }

  def toXML(
    x: Node,
    pscope: NamespaceBinding = TopScope,
    sb: StringBuilder = new StringBuilder,
    stripComments: Boolean = false,
    decodeEntities: Boolean = true,
    preserveWhitespace: Boolean = false,
    minimizeTags: Boolean = false): StringBuilder =
  {
    x match {
      case c: Comment if !stripComments => c buildString sb
      case x: SpecialNode               => x buildString sb
      case g: Group                     => for (c <- g.nodes) toXML(c, x.scope, sb) ; sb
      case _  =>
        // print tag with namespace declarations
        sb.append('<')
        x.nameToString(sb)
        if (x.attributes ne null) x.attributes.buildString(sb)
        x.scope.buildString(sb, pscope)
        if (x.child.isEmpty && minimizeTags)
          // no children, so use short form: <xyz .../>
          sb.append(" />")
        else {
          // children, so use long form: <xyz ...>...</xyz>
          sb.append('>')
          sequenceToXML(x.child, x.scope, sb, stripComments)
          sb.append("</")
          x.nameToString(sb)
          sb.append('>')
        }
    }
  }

  def sequenceToXML(
    children: Seq[Node],
    pscope: NamespaceBinding = TopScope,
    sb: StringBuilder = new StringBuilder,
    stripComments: Boolean = false): Unit =
  {
    if (children.isEmpty) return
    else if (children forall isAtomAndNotText) { // add space
      val it = children.iterator
      val f = it.next
      toXML(f, pscope, sb)
      while (it.hasNext) {
        val x = it.next
        sb.append(' ')
        toXML(x, pscope, sb)
      }
    }
    else children foreach { toXML(_, pscope, sb) }
  }

  /**
   * Returns prefix of qualified name if any.
   *
   * @param name ...
   * @return     ...
   */
  final def prefix(name: String): Option[String] = (name indexOf ':') match {
    case -1   => None
    case i    => Some(name.substring(0, i))
  }

  /**
   * Returns a hashcode for the given constituents of a node
   *
   * @param uri
   * @param label
   * @param attribHashCode
   * @param children
   */
  def hashCode(pre: String, label: String, attribHashCode: Int, scpeHash: Int, children: Seq[Node]) = (
    ( if(pre ne null) {41 * pre.hashCode() % 7} else {0})
    + label.hashCode() * 53
    + attribHashCode * 7
    + scpeHash * 31
    + {
      var c = 0
      val i = children.iterator
      while(i.hasNext) c = c * 41 + i.next.hashCode
      c
    }
  )

  def appendQuoted(s: String): String = sbToString(appendQuoted(s, _))

  /**
   * Appends &quot;s&quot; if string <code>s</code> does not contain &quot;,
   * &apos;s&apos; otherwise.
   *
   * @param s  ...
   * @param sb ...
   * @return   ...
   */
  def appendQuoted(s: String, sb: StringBuilder) = {
    val ch = if (s contains '"') '\'' else '"'
    sb.append(ch).append(s).append(ch)
  }

  /**
   * Appends &quot;s&quot; and escapes and &quot; i s with \&quot;
   *
   * @param s  ...
   * @param sb ...
   * @return   ...
   */
  def appendEscapedQuoted(s: String, sb: StringBuilder): StringBuilder = {
    sb.append('"')
    for (c <- s) c match {
      case '"' => sb.append('\\'); sb.append('"')
      case _   => sb.append(c)
    }
    sb.append('"')
  }

  /**
   * @param s     ...
   * @param index ...
   * @return      ...
   */
  def getName(s: String, index: Int): String = {
    if (index >= s.length) null
    else (s drop index) match {
      case Seq(x, xs @ _*) if isNameStart(x)  => (Array(x) ++ (xs takeWhile isNameChar)).mkString
      case _                                  => ""
    }
  }

  /**
   * Returns <code>null</code> if the value is a correct attribute value,
   * error message if it isn't.
   *
   * @param value ...
   * @return      ...
   */
  def checkAttributeValue(value: String): String = {
    var i = 0
    while (i < value.length) {
      value.charAt(i) match {
        case '<' =>
          return "< not allowed in attribute value";
        case '&' =>
          val n = getName(value, i+1)
          if (n eq null)
            return "malformed entity reference in attribute value ["+value+"]";
          i = i + n.length + 1
          if (i >= value.length || value.charAt(i) != ';')
            return "malformed entity reference in attribute value ["+value+"]";
        case _   =>
      }
      i = i + 1
    }
    null
  }

  /**
   * new
   *
   * @param value ...
   * @return      ...
   */
  def parseAttributeValue(value: String): Seq[Node] = {
    val zs: Seq[Char] = value
    val sb  = new StringBuilder
    var rfb: StringBuilder = null
    val nb = new NodeBuffer()
    val it = zs.iterator
    while (it.hasNext) {
      var c = it.next
      c match {
        case '&' => // entity! flush buffer into text node
          it.next match {
            case '#' =>
              c = it.next
              val theChar = parseCharRef ({ ()=> c },{ () => c = it.next },{s => throw new RuntimeException(s)})
              sb.append(theChar)

            case x =>
              if (rfb eq null) rfb = new StringBuilder()
              rfb.append(x)
              c = it.next
              while (c != ';') {
                rfb.append(c)
                c = it.next
              }
              val ref = rfb.toString()
              rfb.setLength(0)
              unescape(ref,sb) match {
                case null =>
                  if (sb.length > 0) {          // flush buffer
                    nb += Text(sb.toString())
                    sb.setLength(0)
                  }
                  nb += EntityRef(sb.toString()) // add entityref
                case _ =>
              }
          }
        case x   =>
          sb.append(x)
      }
    }
    if (sb.length > 0) { // flush buffer
      val x = Text(sb.toString())
      if (nb.length == 0)
        return x
      else
        nb += x
    }
    nb
  }

  /**
   * <pre>
   *   CharRef ::= "&amp;#" '0'..'9' {'0'..'9'} ";"
   *             | "&amp;#x" '0'..'9'|'A'..'F'|'a'..'f' { hexdigit } ";"
   * </pre>
   * <p>
   *   see [66]
   * <p>
   *
   * @param ch                ...
   * @param nextch            ...
   * @param reportSyntaxError ...
   * @return                  ...
   */
  def parseCharRef(ch: () => Char, nextch: () => Unit, reportSyntaxError: String => Unit): String = {
    val hex  = (ch() == 'x') && { nextch(); true }
    val base = if (hex) 16 else 10
    var i = 0
    while (ch() != ';') {
      ch() match {
        case '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' =>
          i = i * base + ch().asDigit
        case 'a' | 'b' | 'c' | 'd' | 'e' | 'f'
           | 'A' | 'B' | 'C' | 'D' | 'E' | 'F' =>
          if (! hex)
            reportSyntaxError("hex char not allowed in decimal char ref\n" +
                              "Did you mean to write &#x ?")
          else
            i = i * base + ch().asDigit
        case _ =>
          reportSyntaxError("character '" + ch() + "' not allowed in char ref\n")
      }
      nextch()
    }
    new String(io.UTF8Codec.encode(i), "utf8")
  }

}
