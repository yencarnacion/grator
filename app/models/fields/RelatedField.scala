package models.fields

import models.DB.FieldRow

class RelatedField(val field: FieldRow) extends Field{
	def controllerForm: String = {
		val name = field.name
		val fieldType = if(field.required){
			"longNumber"
		} else {
			"optional(longNumber)"
		}
		s""""$name" -> $fieldType"""
	}
	/*
	@select(
                signupForm("profile.country"), 
                options = options(Countries.list),
                '_default -> "--- Choose a country ---",
                '_label -> "Country",
                '_error -> signupForm("profile.country").error.map(_.withMessage("Please select your country"))
            )
	*/

	override def htmlForm: String = {
		val moduleName = field.module.name
		val relatedModuleRow = field.relatedModule.get
		val relatedModule = relatedModuleRow.module
		val relatedRowName = relatedModule.rowName
		val name = field.name
		s"""<legend>
				@Messages("$moduleName.$name")
		   </legend>
		   @select(
		   		rowForm("$name"),
		   		models.DB.$relatedRowName.getOptions,
		   		'default -> "-- select --"
		   )"""
	}

	def fieldTable: String = {
		val name = field.name
		val fieldType = this.fieldType
		val required = if(field.required){", O.NotNull"} else {""}
		s"""def $name = column[$fieldType]("$name"$required)"""
	}
	
	def fieldType: String = "Long"

	def tableIndex: String = {
		val name = field.name
		val relatedName = field.relatedModule.get.name
		val keyName = field.module.name+"_"+relatedName+"_"+name
		val relatedTable = relatedName.capitalize+"Table"
		val varName = if(this.name == name){name+"Id"}else{this.name}
		s"""def $varName = foreignKey("$keyName", $name, $relatedTable)(_.id)"""
	}
}