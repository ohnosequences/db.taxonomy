package ohnosequences.db.taxonomy

import ohnosequences.db.ncbitaxonomy
import ncbitaxonomy.{Version => NCBIVersion, treeDataFile, treeShapeFile}
import ohnosequences.files.digest.DigestFunction
import ohnosequences.s3._
import java.io.File

sealed abstract class Version(val name: String) {
  val inputVersion: NCBIVersion

  override final def toString: String = name
}

case object Version {

  /** All versions of the database */
  val all: Set[Version] = Set(v0_4_0)

  /** This points to the last version */
  val latest: Version = v0_4_0

  case object v0_4_0 extends Version("0.4.0") {
    val inputVersion = NCBIVersion.v0_2_0
  }

  type v0_4_0 = v0_4_0.type
}

case object data {

  /** Folder where we are going to dump data locally */
  val localFolder: Version => File = version => new File(s"./data/${version}")

  /** Returns the S3Folder for a version
    *
    * @param version a [[Version]]
    */
  def prefix(version: Version): S3Folder =
    version match {
      // TODO: Remove this when we release first stable version
      case Version.v0_4_0 =>
        s3"resources.ohnosequences.com" /
          "db" /
          "taxonomy" /
          "unstable" /
          version.toString /
      case _ =>
        s3"resources.ohnosequences.com" /
          "db" /
          "taxonomy" /
          version.toString /
    }

  /** Returns the path of a folder for a [[TreeType]]
    *
    * @param treeType the [[TreeType]]
    */
  def versionFolder(version: Version, treeType: TreeType): File =
    new File(localFolder(version), treeType.toString)

  /** Returns the S3 object containing the mirrored data file for a taxonomy
    * tree (full, good, environmental, unclassified)
    *
    * @param version the [[Version]] of the taxonomy we want
    * @param treeType the [[TreeType]] of the taxonomy we want: TreeType.Full,
    * TreeType.Good, TreeType.Environmental, TreeType.Unclassified
    */
  def treeData(version: Version, treeType: TreeType): S3Object = {
    val ncbiVersion = version.inputVersion

    treeType match {
      case TreeType.Full =>
        ncbitaxonomy.data.treeData(ncbiVersion)
      case _ =>
        prefix(version) / treeType.toString / treeDataFile
    }
  }

  /** Returns the S3 object containing the mirrored shape file for a taxonomy
    * tree (full, good, environmental, unclassified)
    *
    * @param version the [[Version]] of the taxonomy we want
    * @param treeType the [[TreeType]] of the taxonomy we want: TreeType.Full,
    * TreeType.Good, TreeType.Environmental, TreeType.Unclassified
    */
  def treeShape(version: Version, treeType: TreeType): S3Object = {
    val ncbiVersion = version.inputVersion

    treeType match {
      case TreeType.Full =>
        ncbitaxonomy.data.treeShape(ncbiVersion)
      case _ =>
        prefix(version) / treeType.toString / treeShapeFile
    }
  }

  /** A set with all the S3 objects generated in the input version
    *
    * @param version the [[Version]] we want to list the objects for
    */
  def everything(version: Version): Set[S3Object] =
    TreeType.exceptFull.map { treeData(version, _) } | TreeType.exceptFull.map {
      treeShape(version, _)
    }

  val hashingFunction: DigestFunction = DigestFunction.SHA512

  // Local files
  case object local {

    /** Returns the local path of the data file for a taxonomy tree
      * (full, good, environmental, unclassified)
      *
      * @param version the [[Version]] of the taxonomy we want
      * @param treeType the [[TreeType]] of the taxonomy we want: TreeType.Full,
      * TreeType.Good, TreeType.Environmental, TreeType.Unclassified
      */
    def treeData(version: Version, treeType: TreeType): File =
      new File(versionFolder(version, treeType), treeDataFile)

    /** Returns the local path of the shape file for a taxonomy tree
      * (full, good, environmental, unclassified)
      *
      * @param version the [[Version]] of the taxonomy we want
      * @param treeType the [[TreeType]] of the taxonomy we want: TreeType.Full,
      * TreeType.Good, TreeType.Environmental, TreeType.Unclassified
      */
    def treeShape(version: Version, treeType: TreeType): File =
      new File(versionFolder(version, treeType), treeShapeFile)
  }
}
