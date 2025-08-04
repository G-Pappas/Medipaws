package com.example.medipaws

import android.content.Context
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfWriter
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfPCell
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PdfExportService(private val context: Context) {
    
    fun exportMedicineReport(
        pets: List<Pet>,
        entries: List<MedicineEntry>,
        startDate: Date? = null,
        endDate: Date? = null,
        includeCompleted: Boolean = true
    ): File {
        val fileName = "MediPaws_Report_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"
        val file = File(context.getExternalFilesDir(null), fileName)
        
        val document = Document()
        PdfWriter.getInstance(document, FileOutputStream(file))
        document.open()
        
        try {
            // Add title
            val titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20f)
            val title = Paragraph("MediPaws Medicine Report", titleFont)
            title.alignment = Element.ALIGN_CENTER
            title.spacingAfter = 20f
            document.add(title)
            
            // Add export info
            val exportInfo = Paragraph("Generated on: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}")
            exportInfo.alignment = Element.ALIGN_RIGHT
            exportInfo.spacingAfter = 20f
            document.add(exportInfo)
            
            // Filter entries by date range if specified
            val filteredEntries = if (startDate != null && endDate != null) {
                entries.filter { it.dateTime in startDate..endDate }
            } else {
                entries
            }
            
            // Filter by completion status
            val finalEntries = if (!includeCompleted) {
                filteredEntries.filter { it.status == EntryStatus.PENDING }
            } else {
                filteredEntries
            }
            
            // Group entries by pet
            val entriesByPet = finalEntries.groupBy { entry ->
                pets.find { it.id == entry.petId }?.name ?: "Unknown Pet"
            }
            
            // Add pets information
            if (pets.isNotEmpty()) {
                addPetsSection(document, pets)
            }
            
            // Add medicine schedule
            addMedicineScheduleSection(document, entriesByPet)
            
            // Add summary statistics
            addSummarySection(document, finalEntries)
            
        } finally {
            document.close()
        }
        
        return file
    }
    
    private fun addPetsSection(document: Document, pets: List<Pet>) {
        val petsTitle = Paragraph("Pets Information", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16f))
        petsTitle.spacingAfter = 10f
        document.add(petsTitle)
        
        val petsTable = PdfPTable(4)
        petsTable.widthPercentage = 100f
        
        // Add header
        petsTable.addCell(createHeaderCell("Name"))
        petsTable.addCell(createHeaderCell("Species"))
        petsTable.addCell(createHeaderCell("Breed"))
        petsTable.addCell(createHeaderCell("Age"))
        
        // Add pet data
        pets.forEach { pet ->
            petsTable.addCell(createCell(pet.name))
            petsTable.addCell(createCell(pet.species ?: "Unknown"))
            petsTable.addCell(createCell(pet.breed ?: "Unknown"))
            petsTable.addCell(createCell(pet.age?.toString() ?: "Unknown"))
        }
        
        document.add(petsTable)
        document.add(Paragraph(""))
    }
    
    private fun addMedicineScheduleSection(document: Document, entriesByPet: Map<String, List<MedicineEntry>>) {
        val scheduleTitle = Paragraph("Medicine Schedule", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16f))
        scheduleTitle.spacingAfter = 10f
        document.add(scheduleTitle)
        
        entriesByPet.forEach { (petName, entries) ->
            val petTitle = Paragraph("Pet: $petName", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14f))
            petTitle.spacingAfter = 10f
            document.add(petTitle)
            
            val scheduleTable = PdfPTable(5)
            scheduleTable.widthPercentage = 100f
            
            // Add header
            scheduleTable.addCell(createHeaderCell("Date"))
            scheduleTable.addCell(createHeaderCell("Medicine"))
            scheduleTable.addCell(createHeaderCell("Dose"))
            scheduleTable.addCell(createHeaderCell("Status"))
            scheduleTable.addCell(createHeaderCell("Notes"))
            
            // Add medicine data
            entries.sortedBy { it.dateTime }.forEach { entry ->
                scheduleTable.addCell(createCell(SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(entry.dateTime)))
                scheduleTable.addCell(createCell(entry.name))
                scheduleTable.addCell(createCell(entry.dose))
                
                val statusCell = createCell(entry.status.name)
                when (entry.status) {
                    EntryStatus.DONE -> statusCell.backgroundColor = BaseColor.GREEN
                    EntryStatus.LOST -> statusCell.backgroundColor = BaseColor.RED
                    EntryStatus.PENDING -> statusCell.backgroundColor = BaseColor.YELLOW
                }
                scheduleTable.addCell(statusCell)
                
                scheduleTable.addCell(createCell(entry.notes ?: ""))
            }
            
            document.add(scheduleTable)
            document.add(Paragraph(""))
        }
    }
    
    private fun addSummarySection(document: Document, entries: List<MedicineEntry>) {
        val summaryTitle = Paragraph("Summary Statistics", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16f))
        summaryTitle.spacingAfter = 10f
        document.add(summaryTitle)
        
        val totalEntries = entries.size
        val completedEntries = entries.count { it.status == EntryStatus.DONE }
        val pendingEntries = entries.count { it.status == EntryStatus.PENDING }
        val lostEntries = entries.count { it.status == EntryStatus.LOST }
        
        val summaryText = """
            Total Entries: $totalEntries
            Completed: $completedEntries
            Pending: $pendingEntries
            Lost: $lostEntries
            Completion Rate: ${if (totalEntries > 0) "%.1f".format(completedEntries * 100.0 / totalEntries) else "0.0"}%
        """.trimIndent()
        
        val summaryParagraph = Paragraph(summaryText)
        summaryParagraph.spacingAfter = 10f
        document.add(summaryParagraph)
    }
    
    private fun createHeaderCell(text: String): PdfPCell {
        val cell = PdfPCell(Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f)))
        cell.backgroundColor = BaseColor.LIGHT_GRAY
        cell.horizontalAlignment = Element.ALIGN_CENTER
        cell.verticalAlignment = Element.ALIGN_MIDDLE
        cell.setPadding(5f)
        return cell
    }
    
    private fun createCell(text: String): PdfPCell {
        val cell = PdfPCell(Phrase(text))
        cell.horizontalAlignment = Element.ALIGN_LEFT
        cell.verticalAlignment = Element.ALIGN_MIDDLE
        cell.setPadding(5f)
        return cell
    }
} 