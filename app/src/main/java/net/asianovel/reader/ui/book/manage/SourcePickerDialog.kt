package net.asianovel.reader.ui.book.manage

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.setPadding
import androidx.recyclerview.widget.LinearLayoutManager
import net.asianovel.reader.R
import net.asianovel.reader.base.BaseDialogFragment
import net.asianovel.reader.base.adapter.ItemViewHolder
import net.asianovel.reader.base.adapter.RecyclerAdapter
import net.asianovel.reader.data.appDb
import net.asianovel.reader.data.entities.BookSource
import net.asianovel.reader.databinding.DialogSourcePickerBinding
import net.asianovel.reader.databinding.Item1lineTextBinding
import net.asianovel.reader.lib.theme.primaryColor
import net.asianovel.reader.lib.theme.primaryTextColor
import net.asianovel.reader.utils.applyTint
import net.asianovel.reader.utils.dpToPx
import net.asianovel.reader.utils.setLayout
import net.asianovel.reader.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import splitties.views.onClick

class SourcePickerDialog : BaseDialogFragment(R.layout.dialog_source_picker) {

    private val binding by viewBinding(DialogSourcePickerBinding::bind)
    private val searchView: SearchView by lazy {
        binding.toolBar.findViewById(R.id.search_view)
    }
    private val adapter by lazy {
        SourceAdapter(requireContext())
    }
    private var sourceFlowJob: Job? = null

    override fun onStart() {
        super.onStart()
        setLayout(1f, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        initView()
        initData()
    }

    private fun initView() {
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.toolBar.title = "选择书源"
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        searchView.applyTint(primaryTextColor)
        searchView.onActionViewExpanded()
        searchView.isSubmitButtonEnabled = true
        searchView.queryHint = getString(R.string.search_book_source)
        searchView.clearFocus()
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                initData(newText)
                return false
            }
        })
    }

    private fun initData(searchKey: String? = null) {
        sourceFlowJob?.cancel()
        sourceFlowJob = launch {
            when {
                searchKey.isNullOrEmpty() -> appDb.bookSourceDao.flowEnabled()
                else -> appDb.bookSourceDao.flowSearchEnabled(searchKey)
            }.collect {
                adapter.setItems(it)
            }
        }
    }

    inner class SourceAdapter(context: Context) :
        RecyclerAdapter<BookSource, Item1lineTextBinding>(context) {

        override fun getViewBinding(parent: ViewGroup): Item1lineTextBinding {
            return Item1lineTextBinding.inflate(inflater, parent, false).apply {
                root.setPadding(16.dpToPx())
            }
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: Item1lineTextBinding,
            item: BookSource,
            payloads: MutableList<Any>
        ) {
            binding.textView.text = item.getDisPlayNameGroup()
        }

        override fun registerListener(holder: ItemViewHolder, binding: Item1lineTextBinding) {
            binding.root.onClick {
                getItemByLayoutPosition(holder.layoutPosition)?.let {
                    callback?.sourceOnClick(it)
                    dismissAllowingStateLoss()
                }
            }
        }

    }

    private val callback: Callback?
        get() {
            return (parentFragment as? Callback) ?: activity as? Callback
        }

    interface Callback {
        fun sourceOnClick(source: BookSource)
    }

}