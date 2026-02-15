package de.linkel.model.paperless

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.ParseException
import java.util.Locale

// sollen wir da pro Datentyp ne eigene Klasse machen?
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "data_type")
@JsonSubTypes(
    JsonSubTypes.Type(value = StringCustomField::class, name = "string"),
    JsonSubTypes.Type(value = IntegerCustomField::class, name = "integer"),
    JsonSubTypes.Type(value = DateCustomField::class, name = "date"),
    JsonSubTypes.Type(value = MonetaryCustomField::class, name = "monetary"),
    JsonSubTypes.Type(value = SelectCustomField::class, name = "select"),
)
interface CustomField: BaseObj {
    override val slug get() = name.lowercase().replace(" ", "-")
    fun toValue(value: String): CustomFieldValue?
}

@JsonTypeName("string")
@JsonIgnoreProperties(ignoreUnknown = true)
data class StringCustomField(
    override val id: Long,
    override val name: String,
): CustomField {
    override fun toValue(value: String): CustomFieldValue {
        return CustomFieldValue(field = id, value = value)
    }
}

@JsonTypeName("integer")
@JsonIgnoreProperties(ignoreUnknown = true)
data class IntegerCustomField(
    override val id: Long,
    override val name: String,
): CustomField {
    override fun toValue(value: String): CustomFieldValue? {
        return value.toIntOrNull()
            ?.let { CustomFieldValue(field = id, value = it) }
    }
}

@JsonTypeName("date")
@JsonIgnoreProperties(ignoreUnknown = true)
data class DateCustomField(
    override val id: Long,
    override val name: String,
): CustomField {
    override fun toValue(value: String): CustomFieldValue {
        return CustomFieldValue(field = id, value = value)
    }
}

@JsonTypeName("monetary")
@JsonIgnoreProperties(ignoreUnknown = true)
data class MonetaryCustomField(
    override val id: Long,
    override val name: String,
    @JsonProperty("extra_data") val extraData: ExtraData,
): CustomField {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ExtraData(
        @JsonProperty("default_currency") val defaultCurrency: String
    )

    @JsonIgnore
    val parseFormats = listOf(
        DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.GERMANY)),
        DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US)),
    )

    @JsonIgnore
    val outputFormat = DecimalFormat("#####0.00", DecimalFormatSymbols.getInstance(Locale.US))

    val currency get() = extraData.defaultCurrency

    override fun toValue(value: String): CustomFieldValue? {
        return parseFormats
            .firstNotNullOfOrNull { try { it.parse(value) } catch(_: ParseException) { null } }
        ?.let { return CustomFieldValue(field = id, value = "${extraData.defaultCurrency}${outputFormat.format(it)}") }
    }

    constructor(id: Long, name: String, defaultCurrency: String) : this(
        id = id,
        name = name,
        extraData = ExtraData(defaultCurrency = defaultCurrency)
    )
}

@JsonTypeName("select")
@JsonIgnoreProperties(ignoreUnknown = true)
data class SelectCustomField(
    override val id: Long,
    override val name: String,
    @JsonProperty("extra_data") val extraData: ExtraData,
): CustomField {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ExtraData(
        @JsonProperty("select_options") val selectOptions: List<SelectOption> = emptyList(),
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class SelectOption(val id: String, val label: String)

    val selectOptions: List<SelectOption> get() = extraData.selectOptions

    override fun toValue(value: String): CustomFieldValue? {
        return extraData.selectOptions.find { it.label == value }
            ?.let { CustomFieldValue(field = id, value = it.id) }
    }

    constructor(id: Long, name: String, selectOptions: List<SelectOption>) : this(
        id = id,
        name = name,
        extraData = ExtraData(selectOptions = selectOptions)
    )
}
