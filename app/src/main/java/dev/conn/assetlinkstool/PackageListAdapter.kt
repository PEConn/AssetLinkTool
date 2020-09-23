package dev.conn.assetlinkstool

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter
import java.util.*

class PackageListAdapter(context: Context, resource: Int, list: List<String>)
    : ArrayAdapter<String>(context, resource, list) {

    val originalData: List<String>
    var filteredData: List<String>
    val filter: PackageListFilter = PackageListFilter()

    init {
        this.originalData = list
        this.filteredData = list
    }

    inner class PackageListFilter : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filterString = constraint.toString().toLowerCase(Locale.getDefault());

            val filteredList = originalData.filter { it.contains(filterString) }

            val results = FilterResults()
            results.values = filteredList
            results.count = filteredList.size
            return results
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            // TODO: Error checking and such
            filteredData = results!!.values as List<String>
            notifyDataSetChanged()
        }

    }

    override fun getCount(): Int {
        return filteredData.size
    }

    override fun getItem(position: Int): String {
        return filteredData.get(position)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getFilter(): Filter {
        return filter
    }
}