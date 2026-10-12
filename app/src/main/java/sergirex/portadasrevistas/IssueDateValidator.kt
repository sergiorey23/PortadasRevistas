package sergirex.portadasrevistas

import com.google.android.material.datepicker.CalendarConstraints
import kotlinx.parcelize.Parcelize

/**
 * Lets the date picker select only days on which the magazine actually published an issue.
 * [validDays] holds those days as UTC-midnight millis (what the picker reports), sorted ascending.
 */
@Parcelize
class IssueDateValidator(private val validDays: LongArray) : CalendarConstraints.DateValidator {
    override fun isValid(date: Long): Boolean = validDays.binarySearch(date) >= 0
}
