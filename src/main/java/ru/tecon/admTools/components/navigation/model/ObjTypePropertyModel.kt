package ru.tecon.admTools.components.navigation.model

import java.io.Serializable

/**
 * Модель для свойств поиска
 *
 * @author Maksim Shchelkonogov
 * 22.09.2023
 */
data class ObjTypePropertyModel(val objTypeValue: String? = null, val objTypeId: Long = 0, val spHeaderId: Int? = null) : Serializable