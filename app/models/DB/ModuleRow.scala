package models.DB

import play.api.Play.current

import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._
import scala.slick.driver.MySQLDriver.DeleteInvoker
import slick.lifted.{Join, MappedTypeMapper}

import utils.FileUtils

import models.fields._
import models.Module

import play.api.Logger

case class ModuleRow(
	id: Option[Long] = None,
	name: String,
	applicationId: Long
){
  lazy val fields = FieldRow.findByModule(this.id.get)
  lazy val renderFields = for (fieldRow <- this.fields) yield (FieldFactory.get(fieldRow))
  lazy val application = ApplicationRow.findById(this.applicationId)

  lazy val relatedFields: List[RelatedField] = for{
    fieldRow <- this.fields
    if(fieldRow.fieldType == "Related")
  }  yield(new RelatedField(fieldRow))

  lazy val module = new Module(this)

  def getPath(folder: String, fileTermination: String): String = {
    val basePath = this.application.get.path

    val path = basePath+folder+this.name.capitalize+fileTermination
    Logger.debug(path)
    path
  }

  def getViewPath(viewName: String): String = {
    val app = ApplicationRow.findById(this.applicationId)
    val basePath = app.get.path

    basePath+"app/views/"+this.name+"/"+viewName+".scala.html"
  }

  def generateAll(): Unit = {
    this.generateController()
    this.generateTable()
    this.generateRow()
    this.generateViews()
  }

  def generateController(): Unit = {
    val path = this.getPath("app/controllers/","Controller.scala")
    FileUtils.writeToFile(path,views.html.module.template.controller(this.module,this.renderFields,this.relatedFields).toString)
  }

  def generateTable(): Unit = {
    val path = this.getPath("app/models/DB/","Table.scala")
    FileUtils.writeToFile(path,views.html.module.template.table(this.module,this.renderFields, this.relatedFields).toString)
  }

  def generateRow(): Unit = {
    val path = this.getPath("app/models/DB/","Row.scala")
    FileUtils.writeToFile(path,views.html.module.template.row(this.module,this.renderFields).toString)
  }

  def generateDetailView(): Unit = {
    val path = this.getViewPath("detail")
    FileUtils.writeToFile(path,views.html.module.template.moduleviews.detail(this.module).toString)
  }

  def generateEditView(): Unit = {
    val path = this.getViewPath("edit")
    FileUtils.writeToFile(path,views.html.module.template.moduleviews.edit(this.module).toString)
  }

  def generateFormView(): Unit = {
    val path = this.getViewPath("form")
    FileUtils.writeToFile(path,views.html.module.template.moduleviews.form(this.module,this.renderFields).toString)
  }

  def generateIndexView(): Unit = {
    val path = this.getViewPath("index")
    FileUtils.writeToFile(path,views.html.module.template.moduleviews.index(this.module).toString)
  }

  def generateInsertView(): Unit = {
    val path = this.getViewPath("insert")
    FileUtils.writeToFile(path,views.html.module.template.moduleviews.insert(this.module).toString)
  }

  def generateListView(): Unit = {
    val path = this.getViewPath("list")
    FileUtils.writeToFile(path,views.html.module.template.moduleviews.list(this.module,this.renderFields).toString)
  }

  def generateViews(): Unit = {
    this.generateDetailView()
    this.generateEditView()
    this.generateFormView()
    this.generateIndexView()
    this.generateInsertView()
    this.generateListView()
  }
}

object ModuleRow{
  def save(module: ModuleRow):Long = {
    DB.withTransaction { implicit session =>
      val id = ModuleTable.returning(ModuleTable.id).insert(module)

      val moduleId = new FieldRow(None, "id", id, "Id", true)
      FieldTable.insert(moduleId)

      val moduleName = new FieldRow(None, "name", id, "Name", true)
      FieldTable.insert(moduleName)
      
      id
    }
  }
  
  def update(module: ModuleRow):Int = {
    DB.withTransaction { implicit session =>
      //Shows.where(_.id === show.id.get).update(show)
      val q = for {
        s <- ModuleTable
        if s.id === module.id
      } yield(s)
      q.update(module)
    }
  }
  
  def delete(module: ModuleRow):Int = {
    DB.withTransaction { implicit session =>
      //Shows.where(_.id === show.id.get).delete
       val q = for {
        s <- ModuleTable
        if s.id === module.id.get
      } yield(s)
      //q.delete
      (new DeleteInvoker(q)).delete
    }
  }

  
  def findAll: List[ModuleRow] = {
    DB.withSession { implicit session =>
      Query(ModuleTable).list
    }
  }
  
  def findById(id: Long):Option[ModuleRow] = {
    DB.withSession { implicit session =>
      val q = for{
        s <- ModuleTable if s.id === id
      } yield (s)
      q.firstOption
    }
  }

  def findByApplicationId(id: Long): List[ModuleRow] = {
    DB.withSession { implicit session =>
      val q = for{
        s <- ModuleTable if s.applicationId === id
      } yield (s)
      q.list
    }
  }

  def findByApplication(applicationId: Long): List[ModuleRow] = {
    DB.withSession { implicit session =>
      val q = for{
        s <- ModuleTable if s.applicationId === applicationId
      } yield (s)
      q.list
    }
  }

  def getOptions(): Seq[(String,String)] = {
    DB.withSession { implicit session =>
      val modules = for {
        p <- ModuleTable
      } yield(p)
      for(module <- modules.list) yield(module.id.get.toString,module.name) 
    }
  }
}