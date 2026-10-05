package com.example.worldbusiness.data.model

data class CrossBorderInvoiceItem(
    val description: String,
    val quantity: Double,
    val unitPrice: Double,
    val hsnSacCode: String = "998313", // International Information Technology & Architecture SAC code
    val taxRatePercent: Double = 0.0
) {
    val totalAmount: Double get() = quantity * unitPrice
}

data class InternationalClientProfile(
    val name: String,
    val country: String,
    val countryCode: String,
    val address: String,
    val vatTaxId: String,
    val preferredCurrency: String
)

data class FormattedCrossBorderInvoice(
    val invoiceNumber: String,
    val issueDate: String,
    val dueDate: String,
    val paymentTerms: String,
    val status: String,
    val issuingEntity: EntityRecord,
    val clientName: String,
    val clientCountry: String,
    val clientAddress: String,
    val clientVatId: String,
    val currency: String,
    val currencySymbol: String,
    val lineItems: List<CrossBorderInvoiceItem>,
    val subtotal: Double,
    val taxCalculation: CrossBorderTaxCalculation,
    val bankInstitution: String,
    val swiftBic: String,
    val ibanOrAccount: String,
    val clearingRail: String,
    val customsCertification: String
)
