package dev.conn.assetlinkstool

import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.package_list_activity.*


class PackageListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.package_list_activity)

        val packages = getPackages()

        val adapter = PackageListAdapter(this, R.layout.package_list_item, packages)
        packagesListView.adapter = adapter

        packagesListView.onItemClickListener= object: AdapterView.OnItemClickListener {
            override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val p = adapter.getItem(position);

                val intent = Intent(this@PackageListActivity, PackageDetailsActivity::class.java)
                intent.putExtra(PackageDetailsActivity.EXTRA_KEY_PACKAGE, p)

                startActivity(intent)
            }
        }

        searchFilterEditText.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter.filter(s)
            }
        })
    }

    fun getPackages(): List<String> {
        return packageManager.getInstalledApplications(PackageManager.GET_META_DATA).map {
            it.packageName
        }
    }
}
