package net.asianovel.reader.ui.book.toc.rule

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import net.asianovel.reader.R
import net.asianovel.reader.base.BaseDialogFragment
import net.asianovel.reader.base.adapter.ItemViewHolder
import net.asianovel.reader.base.adapter.RecyclerAdapter
import net.asianovel.reader.data.appDb
import net.asianovel.reader.data.entities.TxtTocRule
import net.asianovel.reader.databinding.DialogEditTextBinding
import net.asianovel.reader.databinding.DialogTocRegexBinding
import net.asianovel.reader.databinding.ItemTocRegexBinding
import net.asianovel.reader.lib.dialogs.alert
import net.asianovel.reader.lib.theme.backgroundColor
import net.asianovel.reader.lib.theme.primaryColor
import net.asianovel.reader.model.ReadBook
import net.asianovel.reader.ui.association.ImportTxtTocRuleDialog
import net.asianovel.reader.ui.widget.recycler.ItemTouchCallback
import net.asianovel.reader.ui.widget.recycler.VerticalDivider
import net.asianovel.reader.utils.*
import net.asianovel.reader.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch

/**
 * txt目录规则
 */
class TxtTocRuleDialog() : BaseDialogFragment(R.layout.dialog_toc_regex),
    Toolbar.OnMenuItemClickListener,
    TxtTocRuleEditDialog.Callback {

    constructor(tocRegex: String?) : this() {
        arguments = Bundle().apply {
            putString("tocRegex", tocRegex)
        }
    }

    private val importTocRuleKey = "tocRuleUrl"
    private val viewModel: TxtTocRuleViewModel by viewModels()
    private val binding by viewBinding(DialogTocRegexBinding::bind)
    private val adapter by lazy { TocRegexAdapter(requireContext()) }
    var selectedName: String? = null
    private var durRegex: String? = null

    override fun onStart() {
        super.onStart()
        setLayout(0.9f, 0.8f)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        durRegex = arguments?.getString("tocRegex")
        binding.toolBar.setTitle(R.string.txt_toc_regex)
        binding.toolBar.inflateMenu(R.menu.txt_toc_regex)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.menu.findItem(R.id.menu_split_long_chapter)
            ?.isChecked = ReadBook.book?.getSplitLongChapter() == true
        binding.toolBar.setOnMenuItemClickListener(this)
        initView()
        initData()
    }

    private fun initView() = binding.run {
        recyclerView.addItemDecoration(VerticalDivider(requireContext()))
        recyclerView.adapter = adapter
        val itemTouchCallback = ItemTouchCallback(adapter)
        itemTouchCallback.isCanDrag = true
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(recyclerView)
        tvCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }
        tvOk.setOnClickListener {
            adapter.getItems().forEach { tocRule ->
                if (selectedName == tocRule.name) {
                    val callBack = activity as? CallBack
                    callBack?.onTocRegexDialogResult(tocRule.rule)
                    dismissAllowingStateLoss()
                    return@setOnClickListener
                }
            }
        }
    }

    private fun initData() {
        launch {
            appDb.txtTocRuleDao.observeAll().conflate().collect { tocRules ->
                initSelectedName(tocRules)
                adapter.setItems(tocRules)
            }
        }
    }

    private fun initSelectedName(tocRules: List<TxtTocRule>) {
        if (selectedName == null && durRegex != null) {
            tocRules.forEach {
                if (durRegex == it.rule) {
                    selectedName = it.name
                    return@forEach
                }
            }
            if (selectedName == null) {
                selectedName = ""
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_add -> showDialogFragment(TxtTocRuleEditDialog())
            R.id.menu_default -> viewModel.importDefault()
            R.id.menu_import -> showImportDialog()
            R.id.menu_split_long_chapter -> {
                ReadBook.book?.setSplitLongChapter(!item.isChecked)
                item.isChecked = !item.isChecked
                if (!item.isChecked) context?.longToastOnUi(R.string.need_more_time_load_content)
            }
        }
        return false
    }

    override fun saveTxtTocRule(txtTocRule: TxtTocRule) {
        viewModel.save(txtTocRule)
    }

    @SuppressLint("InflateParams")
    private fun showImportDialog() {
        val aCache = ACache.get(cacheDir = false)
        val defaultUrl = "https://gitee.com/fisher52/YueDuJson/raw/master/myTxtChapterRule.json"
        val cacheUrls: MutableList<String> = aCache
            .getAsString(importTocRuleKey)
            ?.splitNotBlank(",")
            ?.toMutableList()
            ?: mutableListOf()
        if (!cacheUrls.contains(defaultUrl)) {
            cacheUrls.add(0, defaultUrl)
        }
        requireContext().alert(titleResource = R.string.import_on_line) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = "url"
                editView.setFilterValues(cacheUrls)
                editView.delCallBack = {
                    cacheUrls.remove(it)
                    aCache.put(importTocRuleKey, cacheUrls.joinToString(","))
                }
            }
            customView { alertBinding.root }
            okButton {
                val text = alertBinding.editView.text?.toString()
                text?.let {
                    if (!cacheUrls.contains(it)) {
                        cacheUrls.add(0, it)
                        aCache.put(importTocRuleKey, cacheUrls.joinToString(","))
                    }
                    showDialogFragment(ImportTxtTocRuleDialog(it))
                }
            }
            cancelButton()
        }
    }

    inner class TocRegexAdapter(context: Context) :
        RecyclerAdapter<TxtTocRule, ItemTocRegexBinding>(context),
        ItemTouchCallback.Callback {

        override fun getViewBinding(parent: ViewGroup): ItemTocRegexBinding {
            return ItemTocRegexBinding.inflate(inflater, parent, false)
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: ItemTocRegexBinding,
            item: TxtTocRule,
            payloads: MutableList<Any>
        ) {
            binding.apply {
                if (payloads.isEmpty()) {
                    root.setBackgroundColor(context.backgroundColor)
                    rbRegexName.text = item.name
                    titleExample.text = item.example
                    rbRegexName.isChecked = item.name == selectedName
                    swtEnabled.isChecked = item.enable
                } else {
                    rbRegexName.isChecked = item.name == selectedName
                }
            }
        }

        override fun registerListener(holder: ItemViewHolder, binding: ItemTocRegexBinding) {
            binding.apply {
                rbRegexName.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (buttonView.isPressed && isChecked) {
                        selectedName = getItem(holder.layoutPosition)?.name
                        updateItems(0, itemCount - 1, true)
                    }
                }
                swtEnabled.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (buttonView.isPressed) {
                        getItem(holder.layoutPosition)?.let {
                            it.enable = isChecked
                            launch(IO) {
                                appDb.txtTocRuleDao.update(it)
                            }
                        }
                    }
                }
                ivEdit.setOnClickListener {
                    showDialogFragment(TxtTocRuleEditDialog(getItem(holder.layoutPosition)?.id))
                }
                ivDelete.setOnClickListener {
                    getItem(holder.layoutPosition)?.let { item ->
                        launch(IO) {
                            appDb.txtTocRuleDao.delete(item)
                        }
                    }
                }
            }
        }

        private var isMoved = false

        override fun swap(srcPosition: Int, targetPosition: Int): Boolean {
            swapItem(srcPosition, targetPosition)
            isMoved = true
            return super.swap(srcPosition, targetPosition)
        }

        override fun onClearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.onClearView(recyclerView, viewHolder)
            if (isMoved) {
                for ((index, item) in getItems().withIndex()) {
                    item.serialNumber = index + 1
                }
                launch(IO) {
                    appDb.txtTocRuleDao.update(*getItems().toTypedArray())
                }
            }
            isMoved = false
        }
    }

    interface CallBack {
        fun onTocRegexDialogResult(tocRegex: String) {}
    }

}