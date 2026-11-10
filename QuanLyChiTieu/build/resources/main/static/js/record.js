// 9. JS controller for record.html
// File điều khiển trang "Ghi chép giao dịch" (Thêm mới / Chỉnh sửa giao dịch)

let currentType = 'EXPENSE'; // Biến toàn cục: loại giao dịch đang chọn, mặc định là 'EXPENSE' (Chi tiêu)
// 'EXPENSE' or 'INCOME'      // Ghi chú: chỉ nhận 2 giá trị 'EXPENSE' (Chi) hoặc 'INCOME' (Thu)

let editTxId = null; // Biến toàn cục: id của giao dịch đang sửa; null nghĩa là đang ở chế độ Thêm mới

function setRecordType(type) { // Hàm chuyển đổi loại giao dịch (Chi/Thu) và cập nhật giao diện
    currentType = type; // Ghi lại loại giao dịch mới vào biến toàn cục currentType

    const expenseBtn = document.getElementById('type-expense-btn'); // Lấy phần tử nút "Khoản Chi"
    const incomeBtn = document.getElementById('type-income-btn'); // Lấy phần tử nút "Khoản Thu"

    if (type === 'EXPENSE') { // Nếu loại được chọn là Chi tiêu
        // Kích hoạt style cho nút "Chi tiêu", làm mờ nút "Thu nhập"
        expenseBtn.className = "py-3 text-xs font-black uppercase tracking-wider rounded-xl transition-all cursor-pointer bg-white dark:bg-slate-900 text-rose-500 shadow-sm border border-slate-200/20 dark:border-slate-800"; // Gán class "active" (nổi bật, màu đỏ) cho nút Chi
        incomeBtn.className = "py-3 text-xs font-black uppercase tracking-wider rounded-xl transition-all cursor-pointer text-slate-400 dark:text-slate-500 hover:text-slate-700 dark:hover:text-slate-300"; // Gán class "inactive" (mờ) cho nút Thu
    } else { // Ngược lại, loại được chọn là Thu nhập
        // Kích hoạt style cho nút "Thu nhập", làm mờ nút "Chi tiêu"
        incomeBtn.className = "py-3 text-xs font-black uppercase tracking-wider rounded-xl transition-all cursor-pointer bg-white dark:bg-slate-900 text-emerald-500 shadow-sm border border-slate-200/20 dark:border-slate-800"; // Gán class "active" (nổi bật, màu xanh lá) cho nút Thu
        expenseBtn.className = "py-3 text-xs font-black uppercase tracking-wider rounded-xl transition-all cursor-pointer text-slate-400 dark:text-slate-500 hover:text-slate-700 dark:hover:text-slate-300"; // Gán class "inactive" (mờ) cho nút Chi
    }

    // Cập nhật lại dropdown danh mục vì Chi tiêu và Thu nhập có danh sách danh mục khác nhau
    populateCategories(); // Gọi hàm render lại danh sách danh mục tương ứng với loại vừa chọn
}
window.setRecordType = setRecordType; // Gắn hàm vào window để HTML có thể gọi qua onclick="setRecordType(...)"

function populateCategories() { // Hàm đổ danh sách danh mục vào thẻ <select id="tx-category">
    const select = document.getElementById('tx-category'); // Lấy phần tử dropdown danh mục
    select.innerHTML = ''; // Xóa toàn bộ option cũ trước khi render lại

    // Chọn danh sách danh mục tương ứng với loại giao dịch hiện tại
    const categoriesList = currentType === 'EXPENSE' // Nếu đang là Chi tiêu
        ? state.expenseCategories // thì lấy danh sách danh mục chi tiêu từ state
        : state.incomeCategories; // ngược lại lấy danh sách danh mục thu nhập từ state

    // Tạo từng thẻ <option> tương ứng với mỗi danh mục
    categoriesList.forEach(cat => { // Duyệt qua từng danh mục trong danh sách
        const option = document.createElement('option'); // Tạo phần tử <option> mới
        option.value = cat.name; // Giá trị thực (value) là tên gốc của danh mục (dùng để lưu dữ liệu)
        option.innerText = translateCategory(cat.name); // Nhãn hiển thị (text) được dịch theo ngôn ngữ hiện tại
        select.appendChild(option); // Thêm option vừa tạo vào thẻ <select>
    });
}

function initRecordPage() { // Hàm khởi tạo chính, chạy khi trang được load
    const lang = state.language; // Lấy ngôn ngữ hiện tại ('vi' hoặc 'en') từ state
    const t = TRANSLATIONS[lang]; // Lấy bộ từ điển dịch tương ứng với ngôn ngữ hiện tại

    // Translate labels
    // Dịch các nhãn tĩnh trên giao diện theo ngôn ngữ đang chọn (vi/en)
    document.getElementById('back-label').innerText = lang === 'vi' ? 'Quay lại Tổng quan' : 'Back to Dashboard'; // Dịch nhãn nút "Quay lại"
    document.getElementById('type-label').innerText = lang === 'vi' ? 'Phân loại' : 'Classification Type'; // Dịch nhãn "Phân loại"
    document.getElementById('type-expense-btn').innerText = t.expenseLabel; // Dịch nhãn nút "Khoản Chi" từ từ điển
    document.getElementById('type-income-btn').innerText = t.incomeLabel; // Dịch nhãn nút "Khoản Thu" từ từ điển
    document.getElementById('amount-label').innerText = t.amountLabel; // Dịch nhãn "Số tiền"
    document.getElementById('title-label').innerText = t.titleLabel; // Dịch nhãn "Tiêu đề"
    document.getElementById('category-label').innerText = t.categoryLabel; // Dịch nhãn "Danh mục"
    document.getElementById('date-label').innerText = t.dateLabel; // Dịch nhãn "Ngày thực hiện"
    document.getElementById('notes-label').innerText = t.notesLabel; // Dịch nhãn "Ghi chú"
    document.getElementById('tx-notes').placeholder = t.notesPlaceholder; // Dịch placeholder ô ghi chú
    document.getElementById('tx-title').placeholder = t.titlePlaceholder; // Dịch placeholder ô tiêu đề
    document.getElementById('tx-amount').placeholder = t.amountPlaceholder; // Dịch placeholder ô số tiền
    document.getElementById('cancel-btn').innerText = t.cancel; // Dịch nhãn nút "Hủy bỏ"
    document.getElementById('save-btn').innerText = t.save; // Dịch nhãn nút "Lưu"

    // Check URL query parameters for 'edit'
    // Đọc tham số "edit" trên URL (?edit=<id>) để biết có đang sửa giao dịch nào không
    const urlParams = new URLSearchParams(window.location.search); // Phân tích chuỗi query string trên URL
    editTxId = urlParams.get('edit'); // Lấy giá trị tham số "edit" (id giao dịch cần sửa, hoặc null nếu không có)

    // Set default date to today
    // Mặc định ngày giao dịch là ngày hôm nay (định dạng YYYY-MM-DD phù hợp với input type="date")
    const today = new Date().toISOString().split('T')[0]; // Lấy ngày hôm nay, cắt chuỗi ISO để chỉ giữ phần YYYY-MM-DD
    document.getElementById('tx-date').value = today; // Gán ngày hôm nay làm giá trị mặc định cho ô ngày

    if (editTxId) { // Nếu có id giao dịch trên URL -> đang ở chế độ Chỉnh sửa
        // Mode: Edit
        // Chế độ Chỉnh sửa: đổi tiêu đề form và tìm giao dịch cần sửa trong state
        document.getElementById('ader').innerText = t.editRecordHeader; // Đổi tiêu đề form thành "Chỉnh sửa giao dịch"

        const txToEdit = state.transactions.find(tx => tx.id === editTxId); // Tìm giao dịch có id trùng khớp trong danh sách state.transactions
        if (txToEdit) { // Nếu tìm thấy giao dịch cần sửa
            // Nếu tìm thấy giao dịch: đổ toàn bộ dữ liệu cũ lên các trường của form
            currentType = txToEdit.type; // Gán lại loại giao dịch (Chi/Thu) theo dữ liệu cũ
            setRecordType(currentType); // Cập nhật UI loại giao dịch + render lại danh mục tương ứng

            document.getElementById('tx-amount').value = txToEdit.amount; // Đổ số tiền cũ lên ô nhập số tiền
            document.getElementById('tx-title').value = txToEdit.title; // Đổ tiêu đề cũ lên ô nhập tiêu đề
            document.getElementById('tx-date').value = txToEdit.date; // Đổ ngày cũ lên ô chọn ngày
            document.getElementById('tx-notes').value = txToEdit.notes || ''; // Đổ ghi chú cũ lên ô ghi chú (nếu không có thì để rỗng)

            // Delay selecting the category until population finishes
            // Trì hoãn việc set giá trị danh mục vì populateCategories() (gọi bên trong setRecordType)
            // cần thời gian để render xong các option trước khi có thể gán value
            setTimeout(() => { // Đặt lịch chạy hàm sau 50ms để chắc chắn dropdown đã có đủ option
                document.getElementById('tx-category').value = txToEdit.category; // Chọn đúng danh mục cũ trong dropdown
            }, 50); // Thời gian trì hoãn: 50 mili-giây
        } else { // Nếu không tìm thấy giao dịch (id không hợp lệ)
            // Không tìm thấy giao dịch (có thể id không hợp lệ) -> báo lỗi và điều hướng về dashboard
            showToast(lang === 'vi' ? 'Giao dịch không tồn tại!' : 'Transaction does not exist!', 'error'); // Hiện thông báo lỗi dạng toast
            setTimeout(() => window.location.href = 'dashboard.html', 1000); // Sau 1 giây, tự động chuyển hướng về trang Dashboard
        }
    } else { // Nếu không có id giao dịch trên URL -> đang ở chế độ Thêm mới
        // Mode: Create
        // Chế độ Thêm mới: đặt tiêu đề form và mặc định loại giao dịch là Chi tiêu
        document.getElementById('ader').innerText = t.addRecordHeader; // Đổi tiêu đề form thành "Ghi chép giao dịch mới"
        setRecordType('EXPENSE'); // Mặc định chọn loại giao dịch là Chi tiêu khi mở form thêm mới
    }

    // Handle form submission
    // Xử lý sự kiện submit form (khi bấm nút Lưu)
    const form = document.getElementById('record-form'); // Lấy phần tử <form> của trang
    form.addEventListener('submit', (e) => { // Lắng nghe sự kiện submit của form
        e.preventDefault(); // Ngăn trình duyệt load lại trang theo hành vi mặc định khi submit form

        // Lấy dữ liệu người dùng nhập từ các trường trên form
        const amount = Number(document.getElementById('tx-amount').value); // Lấy số tiền, ép kiểu về Number
        const title = document.getElementById('tx-title').value.trim(); // Lấy tiêu đề, loại bỏ khoảng trắng thừa 2 đầu
        const category = document.getElementById('tx-category').value; // Lấy danh mục đã chọn
        const date = document.getElementById('tx-date').value; // Lấy ngày đã chọn
        const notes = document.getElementById('tx-notes').value.trim(); // Lấy ghi chú, loại bỏ khoảng trắng thừa 2 đầu

        // Validate dữ liệu đầu vào: số tiền phải > 0 và các trường bắt buộc không được để trống
        if (amount <= 0 || !title || !category || !date) { // Kiểm tra hợp lệ: số tiền phải dương, và các trường không được rỗng
            showToast(lang === 'vi' ? 'Vui lòng điền đủ các thông tin hợp lệ!' : 'Please fill in all valid fields!', 'error'); // Báo lỗi nếu dữ liệu không hợp lệ
            return; // Dừng hàm, không lưu giao dịch
        }

        // Tạo object giao dịch mới từ dữ liệu đã nhập
        // Nếu đang sửa (editTxId có giá trị) thì giữ nguyên id cũ, ngược lại tạo id mới dựa trên timestamp
        const currentTx = { // Tạo object đại diện cho giao dịch hiện tại
            id: editTxId || `tx-${Date.now()}`, // Nếu đang sửa thì giữ id cũ, nếu thêm mới thì sinh id mới theo thời gian hiện tại
            title, // Tiêu đề giao dịch (viết tắt title: title)
            amount, // Số tiền giao dịch
            type: currentType, // Loại giao dịch (Chi/Thu) đang chọn
            category, // Danh mục đã chọn
            date, // Ngày thực hiện
            notes // Ghi chú thêm
        };

        if (editTxId) { // Nếu đang ở chế độ sửa
            // Replace existing item
            // Chế độ sửa: tìm vị trí giao dịch cũ trong mảng và thay thế bằng dữ liệu mới
            const index = state.transactions.findIndex(tx => tx.id === editTxId); // Tìm vị trí (index) của giao dịch cũ trong mảng
            if (index !== -1) { // Nếu tìm thấy vị trí hợp lệ
                state.transactions[index] = currentTx; // Ghi đè giao dịch cũ bằng dữ liệu mới tại đúng vị trí đó
            }
        } else { // Nếu đang ở chế độ thêm mới
            // Add new item to front of the stack
            // Chế độ thêm mới: đưa giao dịch mới lên đầu danh sách (để hiển thị mới nhất lên trước)
            state.transactions.unshift(currentTx); // Thêm giao dịch mới vào đầu mảng transactions
        }

        // Save transaction state back to browser memory
        // Lưu lại toàn bộ state (bao gồm danh sách giao dịch) vào bộ nhớ lưu trữ của trình duyệt
        saveUserData(); // Gọi hàm lưu state (định nghĩa ở state.js) để ghi dữ liệu xuống bộ nhớ lưu trữ

        // Check Budget exceeded warnings
        // Nếu là giao dịch Chi tiêu, kiểm tra xem đã vượt hạn mức ngân sách (budget) của danh mục này chưa
        if (currentType === 'EXPENSE') { // Chỉ kiểm tra ngân sách nếu đây là giao dịch Chi tiêu
            // Tìm ngân sách đã thiết lập cho danh mục tương ứng
            const budgetLimit = state.budgets.find(b => b.category === category); // Tìm hạn mức ngân sách của danh mục vừa chọn
            if (budgetLimit) { // Nếu danh mục này đã có thiết lập ngân sách
                // Calculate total spent on this category this month
                // Tính tổng số tiền đã chi cho danh mục này trong CÙNG THÁNG với ngày giao dịch vừa nhập
                const todayObj = new Date(date); // Chuyển chuỗi ngày thành đối tượng Date
                const curYear = todayObj.getFullYear(); // Lấy năm của ngày giao dịch
                const curMonth = todayObj.getMonth(); // Lấy tháng của ngày giao dịch (0-11)

                let totalSpent = 0; // Biến tích lũy tổng số tiền đã chi trong tháng cho danh mục này
                state.transactions.forEach(tx => { // Duyệt qua toàn bộ giao dịch hiện có
                    if (tx.type === 'EXPENSE' && tx.category === category) { // Chỉ xét các giao dịch Chi tiêu cùng danh mục
                        const txD = new Date(tx.date); // Chuyển ngày của giao dịch đang xét thành đối tượng Date
                        // Chỉ cộng dồn các giao dịch nằm trong cùng tháng/năm với giao dịch vừa lưu
                        if (txD.getFullYear() === curYear && txD.getMonth() === curMonth) { // Kiểm tra cùng năm và cùng tháng
                            totalSpent += Number(tx.amount); // Cộng dồn số tiền vào biến tổng
                        }
                    }
                });

                // So sánh tổng chi tiêu với hạn mức ngân sách để quyết định thông báo cảnh báo hay thành công
                if (totalSpent > budgetLimit.limit) { // Nếu tổng đã chi vượt quá hạn mức ngân sách
                    showToast(`${t.alertOverhead}: ${category} (${formatVND(totalSpent)} / ${formatVND(budgetLimit.limit)})`, 'warning'); // Hiện cảnh báo vượt ngân sách kèm số liệu
                } else { // Nếu chưa vượt hạn mức
                    showToast(lang === 'vi' ? 'Lưu giao dịch thành công!' : 'Transaction saved successfully!'); // Hiện thông báo lưu thành công
                }
            } else { // Nếu danh mục này chưa thiết lập ngân sách
                // Danh mục này chưa thiết lập ngân sách -> chỉ thông báo lưu thành công
                showToast(lang === 'vi' ? 'Lưu giao dịch thành công!' : 'Transaction saved successfully!'); // Hiện thông báo lưu thành công
            }
        } else { // Nếu là giao dịch Thu nhập
            // Giao dịch Thu nhập không cần kiểm tra ngân sách
            showToast(lang === 'vi' ? 'Lưu giao dịch thành công!' : 'Transaction saved successfully!'); // Hiện thông báo lưu thành công
        }

        // Redirect back to transactions ledger or dashboard
        // Sau khi lưu, tự động điều hướng người dùng:
        // - Nếu vừa sửa giao dịch -> quay về trang danh sách giao dịch (transactions.html)
        // - Nếu vừa thêm mới -> quay về trang Tổng quan (dashboard.html)
        setTimeout(() => { // Trì hoãn 1.5 giây để người dùng kịp đọc thông báo toast
            window.location.href = editTxId ? 'transactions.html' : 'dashboard.html'; // Điều hướng tùy theo chế độ sửa hay thêm mới
        }, 1500); // Thời gian trì hoãn: 1500 mili-giây
    });
}

// Khởi chạy trang: nếu DOM đã load xong thì chạy ngay, nếu chưa thì đợi sự kiện DOMContentLoaded
if (document.readyState !== 'loading') { // Kiểm tra xem HTML đã được phân tích (parse) xong chưa
    injectSharedLayout('record'); // Chèn layout dùng chung (sidebar, header...) cho trang record
    initRecordPage(); // Gọi hàm khởi tạo chính của trang
} else { // Nếu HTML chưa load xong
    document.addEventListener('DOMContentLoaded', () => { // Đợi sự kiện DOMContentLoaded (HTML đã sẵn sàng)
        injectSharedLayout('record'); // Chèn layout dùng chung cho trang record
        initRecordPage(); // Gọi hàm khởi tạo chính của trang
    });
}