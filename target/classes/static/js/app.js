document.addEventListener('DOMContentLoaded', function() {
    document.querySelectorAll('.nav-toggle').forEach(function(el) {
        el.addEventListener('click', function() {
            var nav = el.closest('.navbar');
            var links = nav ? nav.querySelector('.nav-links') : document.querySelector('.nav-links');
            if (links) links.classList.toggle('active');
        });
    });
    document.querySelectorAll('.navbar .nav-links .nav-item').forEach(function (item) {
        item.addEventListener('click', function () {
            var nav = item.closest('.navbar');
            var links = nav ? nav.querySelector('.nav-links') : document.querySelector('.nav-links');
            if (links) links.classList.remove('active');
        });
    });
    document.addEventListener('click', function (e) {
        document.querySelectorAll('.navbar').forEach(function (nav) {
            var links = nav.querySelector('.nav-links');
            if (!links || !links.classList.contains('active')) return;
            if (!e.target.closest('.nav-links') && !e.target.closest('.nav-toggle')) {
                links.classList.remove('active');
            }
        });
    });
    document.addEventListener('keydown', function (e) {
        if (e.key !== 'Escape') return;
        document.querySelectorAll('.navbar .nav-links.active').forEach(function (links) {
            links.classList.remove('active');
        });
    });
});

function getCsrfToken() {
    const match = document.cookie.match(/XSRF-TOKEN=([^;]+)/);
    return match ? decodeURIComponent(match[1]) : null;
}

function fetchOpts(method, body) {
    const opts = { method: method || 'GET', credentials: 'same-origin', headers: {} };
    const token = getCsrfToken();
    if (token) opts.headers['X-XSRF-TOKEN'] = token;
    if (body) opts.headers['Content-Type'] = 'application/x-www-form-urlencoded';
    opts.body = body;
    return opts;
}

function showNotification(message, type) {
    var n = document.createElement('div');
    n.className = 'notification ' + (type || 'info');
    var icon = type === 'success' ? 'fa-check-circle' : type === 'error' ? 'fa-exclamation-circle' : 'fa-info-circle';
    n.innerHTML = '<i class="fas ' + icon + '"></i> ' + message;
    document.body.appendChild(n);
    setTimeout(function() { n.classList.add('fade-out'); setTimeout(function() { n.remove(); }, 300); }, 3000);
}

function timeAgo(timestamp) {
    if (!timestamp) return 'just now';
    var d = new Date(timestamp);
    var s = Math.floor((new Date() - d) / 1000);
    if (s >= 31536000) return Math.floor(s/31536000) + ' years ago';
    if (s >= 2592000) return Math.floor(s/2592000) + ' months ago';
    if (s >= 86400) return Math.floor(s/86400) + ' days ago';
    if (s >= 3600) return Math.floor(s/3600) + ' hours ago';
    if (s >= 60) return Math.floor(s/60) + ' minutes ago';
    return 'just now';
}

function likePost(btn) {
    var postId = btn.getAttribute('data-post-id') || (btn.closest && btn.closest('[data-post-id]') && btn.closest('[data-post-id]').getAttribute('data-post-id'));
    if (!postId) return;
    var likeBtn = btn.classList && btn.classList.contains('like-btn') ? btn : btn.closest('.post').querySelector('.like-btn');
    var isLiked = likeBtn.classList.contains('liked');
    var action = isLiked ? 'unlike' : 'like';
    fetch('/api/likes?action=' + action + '&post_id=' + postId, fetchOpts('GET'))
        .then(function(r) { return r.json(); })
        .then(function(data) {
            if (data.success) {
                likeBtn.classList.toggle('liked');
                var icon = likeBtn.querySelector('i');
                icon.classList.toggle('fas'); icon.classList.toggle('far');
                likeBtn.querySelector('.like-count').textContent = data.count;
            }
        });
}

var currentPostId = null;
document.addEventListener('click', function(e) {
    if (e.target.closest('.delete-post-btn')) {
        currentPostId = e.target.closest('.delete-post-btn').getAttribute('data-post-id');
        document.getElementById('deleteModal').style.display = 'block';
    }
});
function closeDeleteModal() {
    document.getElementById('deleteModal').style.display = 'none';
    currentPostId = null;
}
window.closeDeleteModal = closeDeleteModal;
function confirmDelete() {
    if (!currentPostId) return;
    fetch('/api/delete_post', fetchOpts('POST', 'post_id=' + currentPostId))
        .then(function(r) { return r.json(); })
        .then(function(data) {
            if (data.success) {
                var post = document.querySelector('.post[data-post-id="' + currentPostId + '"]');
                if (post) post.remove();
                closeDeleteModal();
            }
        });
}
window.confirmDelete = confirmDelete;
window.addEventListener('click', function(e) {
    if (e.target.id === 'deleteModal') closeDeleteModal();
});

function toggleComments(btn) {
    var postId = btn.getAttribute('data-post-id');
    var section = document.getElementById('comments-' + postId);
    var container = document.getElementById('comments-container-' + postId);
    if (!section || !container) return;
    if (section.style.display === 'none' || !section.style.display) {
        section.style.display = 'block';
        container.innerHTML = '<div class="loading-comments"><i class="fas fa-spinner fa-spin"></i> Loading...</div>';
        fetch('/api/get_comments?post_id=' + postId, fetchOpts('GET'))
            .then(function(r) { return r.json(); })
            .then(function(data) {
                if (data.success && data.comments && data.comments.length) {
                    container.innerHTML = data.comments.map(function(c) {
                        var avatarPath = c.profilePicture || 'assets/logo.png';
                        if (avatarPath && !avatarPath.startsWith('/')) {
                            avatarPath = '/' + avatarPath;
                        }
                        return '<div class="comment" data-comment-id="' + c.id + '">' +
                            '<img src="' + avatarPath + '" class="comment-avatar"/>' +
                            '<div class="comment-content-wrapper"><div class="comment-header">' +
                            '<a href="/profile?id=' + c.userId + '" class="comment-username">' + (c.username || '') + '</a>' +
                            '<span class="comment-time">' + timeAgo(c.createdAt) + '</span></div>' +
                            '<div class="comment-content">' + (c.content || '').replace(/</g,'&lt;').replace(/>/g,'&gt;') + '</div></div></div>';
                    }).join('');
                } else {
                    container.innerHTML = '<p class="no-comments">No comments yet.</p>';
                }
            });
    } else {
        section.style.display = 'none';
    }
}
window.toggleComments = toggleComments;

function addComment(formEl) {
    var postId = formEl.getAttribute('data-post-id');
    var input = document.getElementById('comment-input-' + postId) || formEl.querySelector('.comment-input');
    var text = input && input.value ? input.value.trim() : '';
    if (!text) return false;
    var fd = 'comment=' + encodeURIComponent(text);
    fetch('/api/comments?action=add&post_id=' + postId, fetchOpts('POST', fd))
        .then(function(r) { return r.json(); })
        .then(function(data) {
            if (data.success && data.comment) {
                input.value = '';
                var container = document.getElementById('comments-container-' + postId);
                var noMsg = container.querySelector('.no-comments');
                if (noMsg) container.innerHTML = '';
                var c = data.comment;
                var avatarPath = c.profilePicture || 'assets/logo.png';
                if (avatarPath && !avatarPath.startsWith('/')) {
                    avatarPath = '/' + avatarPath;
                }
                var html = '<div class="comment" data-comment-id="' + c.id + '">' +
                    '<img src="' + avatarPath + '" class="comment-avatar"/>' +
                    '<div class="comment-content-wrapper"><div class="comment-header">' +
                    '<a href="/profile?id=' + c.userId + '" class="comment-username">' + (c.username || '') + '</a>' +
                    '<span class="comment-time">' + timeAgo(c.createdAt) + '</span></div>' +
                    '<div class="comment-content">' + (c.content || '').replace(/</g,'&lt;').replace(/>/g,'&gt;') + '</div></div></div>';
                container.insertAdjacentHTML('afterbegin', html);
                var countEl = formEl.closest('.post').querySelector('.comment-count');
                if (countEl) countEl.textContent = parseInt(countEl.textContent || 0) + 1;
            }
        });
    return false;
}

// --- Direct Messages (DM) ---
var dmClient = null;
var dmConnected = false;
var dmCurrentFriendId = null;
var dmCurrentFriendName = null;
var dmTypingTimeout = null;
var dmRemoteTypingTimeout = null;

function connectDmIfNeeded() {
    if (dmConnected || typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
        return;
    }
    var socket = new SockJS('/ws');
    dmClient = Stomp.over(socket);
    dmClient.debug = null;
    dmClient.connect({}, function () {
        dmConnected = true;
        dmClient.subscribe('/user/queue/messages', function (message) {
            var body = JSON.parse(message.body || '{}');
            handleIncomingDm(body);
        });
        dmClient.subscribe('/user/queue/typing', function (message) {
            var body = JSON.parse(message.body || '{}');
            handleIncomingTyping(body);
        });
    });
}

function toggleDmPanel() {
    var panel = document.getElementById('dm-panel');
    if (!panel) return;
    if (panel.style.display === 'none' || !panel.style.display) {
        panel.style.display = 'flex';
        connectDmIfNeeded();
        loadDmFriends();
    } else {
        panel.style.display = 'none';
    }
}
window.toggleDmPanel = toggleDmPanel;

function loadDmFriends() {
    fetch('/api/dm/friends', fetchOpts('GET'))
        .then(function (r) { return r.json(); })
        .then(function (data) {
            if (!data.success) return;
            var list = document.getElementById('dm-friends-list');
            if (!list) return;
            list.innerHTML = '';
            (data.friends || []).forEach(function (f) {
                var avatar = f.profilePicture || 'assets/logo.png';
                if (avatar && !avatar.startsWith('/')) avatar = '/' + avatar;
                var item = document.createElement('div');
                item.className = 'dm-friend-item';
                item.setAttribute('data-friend-id', f.id);
                item.setAttribute('data-friend-name', f.username || '');
                item.innerHTML = '<img class="dm-friend-avatar" src="' + avatar + '"/>' +
                    '<span class="dm-friend-name">' + (f.username || '') + '</span>';
                item.addEventListener('click', function () {
                    document.querySelectorAll('.dm-friend-item.active').forEach(function (el) {
                        el.classList.remove('active');
                    });
                    item.classList.add('active');
                    dmCurrentFriendId = f.id;
                    dmCurrentFriendName = f.username || '';
                    var header = document.getElementById('dm-chat-header');
                    if (header) header.textContent = 'Chat with ' + dmCurrentFriendName;
                    loadDmHistory();
                });
                list.appendChild(item);
            });
        });
}

function loadDmHistory() {
    if (!dmCurrentFriendId) return;
    fetch('/api/dm/history?friendId=' + dmCurrentFriendId, fetchOpts('GET'))
        .then(function (r) { return r.json(); })
        .then(function (data) {
            if (!data.success) return;
            var box = document.getElementById('dm-messages');
            if (!box) return;
            box.innerHTML = '';
            (data.messages || []).forEach(function (m) {
                appendDmMessage(m);
            });
            box.scrollTop = box.scrollHeight;
        });
}

function appendDmMessage(msg) {
    var box = document.getElementById('dm-messages');
    if (!box || !msg) return;
    hideTypingIndicator();
    var item = document.createElement('div');
    var isOutgoing = msg.senderId && dmCurrentFriendId && String(msg.senderId) !== String(dmCurrentFriendId);
    item.className = 'dm-message ' + (isOutgoing ? 'dm-message-outgoing' : 'dm-message-incoming');
    item.textContent = msg.content || '';
    box.appendChild(item);
    box.scrollTop = box.scrollHeight;
}

function handleIncomingDm(msg) {
    if (!msg) return;
    // Show only if conversation with this friend is active
    if (dmCurrentFriendId && (String(msg.senderId) === String(dmCurrentFriendId) ||
        String(msg.receiverId) === String(dmCurrentFriendId))) {
        appendDmMessage(msg);
    } else {
        // Only show notification if it's NOT from ourselves
        if (msg.senderName !== document.querySelector('.navbar .nav-links span')?.innerText) {
            showNotification('New message from ' + (msg.senderName || 'friend'), 'info');
        }
    }
}

function sendDmMessage() {
    var input = document.getElementById('dm-input');
    if (!input || !dmClient || !dmConnected || !dmCurrentFriendId) return false;
    var text = input.value ? input.value.trim() : '';
    if (!text) return false;
    dmClient.send('/app/chat.send', {}, JSON.stringify({
        receiverId: dmCurrentFriendId,
        content: text
    }));
    hideTypingIndicator();
    input.value = '';
    return false;
}
window.sendDmMessage = sendDmMessage;

function showTypingIndicator(name) {
    var box = document.getElementById('dm-messages');
    if (!box) return;
    var existing = document.getElementById('dm-typing-indicator');
    if (existing) {
        box.scrollTop = box.scrollHeight;
        return;
    }
    var item = document.createElement('div');
    item.id = 'dm-typing-indicator';
    item.className = 'dm-typing';
    item.innerHTML = '<span class="dm-typing-label">' + (name || 'Typing') + '</span>' +
        '<div class="dm-typing-bubble"><span></span><span></span><span></span></div>';
    box.appendChild(item);
    box.scrollTop = box.scrollHeight;
}

function hideTypingIndicator() {
    var existing = document.getElementById('dm-typing-indicator');
    if (existing) existing.remove();
}

function handleIncomingTyping(payload) {
    if (!payload || !dmCurrentFriendId) return;
    if (String(payload.senderId) !== String(dmCurrentFriendId)) return;
    if (!payload.typing) {
        hideTypingIndicator();
        return;
    }
    showTypingIndicator((payload.senderName || dmCurrentFriendName || 'Typing') + ' is typing');
    if (dmRemoteTypingTimeout) clearTimeout(dmRemoteTypingTimeout);
    dmRemoteTypingTimeout = setTimeout(hideTypingIndicator, 1600);
}

function notifyTyping(isTyping) {
    if (!dmClient || !dmConnected || !dmCurrentFriendId) return;
    try {
        dmClient.send('/app/chat.typing', {}, JSON.stringify({
            receiverId: dmCurrentFriendId,
            typing: !!isTyping
        }));
    } catch (e) {
        // Ignore if typing endpoint is not supported server-side.
    }
}

function initDmTyping() {
    var input = document.getElementById('dm-input');
    if (!input || input.dataset.typingBound === '1') return;
    input.dataset.typingBound = '1';
    input.addEventListener('input', function () {
        var hasText = input.value && input.value.trim().length > 0;
        if (hasText) {
            notifyTyping(true);
            if (dmTypingTimeout) clearTimeout(dmTypingTimeout);
            dmTypingTimeout = setTimeout(function () {
                notifyTyping(false);
            }, 1000);
        } else {
            notifyTyping(false);
        }
    });
}

function initPasswordToggles() {
    document.querySelectorAll('.password-toggle-btn').forEach(function (btn) {
        if (btn.dataset.bound === '1') return;
        btn.dataset.bound = '1';
        btn.addEventListener('click', function () {
            var wrap = btn.closest('.password-field-wrap');
            if (!wrap) return;
            var input = wrap.querySelector('input[type="password"], input[type="text"]');
            if (!input) return;
            var isHidden = input.type === 'password';
            input.type = isHidden ? 'text' : 'password';
            btn.setAttribute('aria-pressed', isHidden ? 'true' : 'false');
            btn.setAttribute('aria-label', isHidden ? 'Hide password' : 'Show password');
            var icon = btn.querySelector('i');
            if (icon) {
                icon.className = isHidden ? 'fas fa-eye-slash' : 'fas fa-eye';
            }
        });
    });
}

function expressifyPasswordPolicyMet(pwd) {
    if (!pwd || pwd.length <= 8) return false;
    if (!/[A-Z]/.test(pwd)) return false;
    if (!/[a-z]/.test(pwd)) return false;
    if (!/[0-9]/.test(pwd)) return false;
    if (!/[^A-Za-z0-9]/.test(pwd)) return false;
    return true;
}

function expressifyPasswordStrengthLevel(pwd) {
    pwd = pwd || '';
    if (!pwd.length) return 'none';
    if (expressifyPasswordPolicyMet(pwd)) return 'strong';
    var score = 0;
    if (pwd.length > 8) score++;
    if (/[A-Z]/.test(pwd)) score++;
    if (/[a-z]/.test(pwd)) score++;
    if (/[0-9]/.test(pwd)) score++;
    if (/[^A-Za-z0-9]/.test(pwd)) score++;
    if (score <= 2) return 'weak';
    return 'medium';
}

function updatePasswordStrengthMeter(pwd, fillEl, textEl) {
    var level = expressifyPasswordStrengthLevel(pwd);
    if (!fillEl || !textEl) return;
    fillEl.className = 'password-strength-fill';
    textEl.className = 'password-strength-text';
    if (level === 'none') {
        fillEl.style.width = '0%';
        textEl.textContent = 'Password strength';
        return;
    }
    fillEl.style.width = '';
    fillEl.classList.add(level);
    var labels = { weak: 'Weak', medium: 'Medium', strong: 'Strong' };
    textEl.textContent = labels[level] || '';
    textEl.classList.add('level-' + level);
}

function updatePasswordRulesList(rulesContainer, pwd) {
    if (!rulesContainer) return;
    pwd = pwd || '';
    var checks = {
        len: pwd.length > 8,
        upper: /[A-Z]/.test(pwd),
        lower: /[a-z]/.test(pwd),
        num: /[0-9]/.test(pwd),
        special: /[^A-Za-z0-9]/.test(pwd)
    };
    rulesContainer.querySelectorAll('[data-rule]').forEach(function (li) {
        var k = li.getAttribute('data-rule');
        var ok = checks[k];
        li.classList.toggle('rule-pass', !!ok);
        var icon = li.querySelector('i');
        if (icon) {
            icon.className = ok ? 'fas fa-check' : 'fas fa-circle';
        }
    });
}

function wirePasswordStrength(input, fillId, textId, rulesId) {
    var fillEl = document.getElementById(fillId);
    var textEl = document.getElementById(textId);
    var rulesEl = rulesId ? document.getElementById(rulesId) : null;
    function refresh() {
        var v = input.value;
        updatePasswordStrengthMeter(v, fillEl, textEl);
        updatePasswordRulesList(rulesEl, v);
    }
    input.addEventListener('input', refresh);
    input.addEventListener('focus', refresh);
    refresh();
}

function initPasswordStrengthMeters() {
    var reg = document.getElementById('reg_password');
    if (reg) {
        wirePasswordStrength(reg, 'reg_password_strength_fill', 'reg_password_strength_text', 'reg_password_rules');
    }
    var settingsNew = document.getElementById('settings_new_password');
    if (settingsNew) {
        wirePasswordStrength(settingsNew, 'settings_password_strength_fill', 'settings_password_strength_text',
            'settings_password_rules');
    }
}

function initRegisterPasswordSubmitGuard() {
    var form = document.getElementById('register-form');
    var pwd = document.getElementById('reg_password');
    if (!form || !pwd) return;
    form.addEventListener('submit', function (e) {
        if (!expressifyPasswordPolicyMet(pwd.value)) {
            e.preventDefault();
            showNotification('Choose a stronger password: use 9+ characters with upper & lower case, a number, and a special character.', 'error');
        }
    });
}

function initSettingsPasswordSubmitGuard() {
    var form = document.querySelector('.settings-form form[action*="password"]');
    var newPw = document.getElementById('settings_new_password');
    if (!form || !newPw) return;
    form.addEventListener('submit', function (e) {
        if (!expressifyPasswordPolicyMet(newPw.value)) {
            e.preventDefault();
            showNotification('New password must be 9+ characters and include upper & lower case, a number, and a special character.', 'error');
        }
    });
}

document.addEventListener('DOMContentLoaded', function () {
    initPasswordToggles();
    initPasswordStrengthMeters();
    initRegisterPasswordSubmitGuard();
    initSettingsPasswordSubmitGuard();
    initDmTyping();
    initEscapeClosesModals();
    initHomeFeedEnhancements();
});

function initEscapeClosesModals() {
    document.addEventListener('keydown', function (e) {
        if (e.key !== 'Escape') return;
        var delModal = document.getElementById('deleteModal');
        if (delModal && delModal.style.display === 'block' && typeof closeDeleteModal === 'function') {
            closeDeleteModal();
        }
        var mod = document.getElementById('moderationModal');
        if (mod && mod.style.display !== 'none' && typeof window.closeModerationModal === 'function') {
            window.closeModerationModal();
        }
    });
}

function initHomeFeedEnhancements() {
    if (!document.body.classList.contains('page-home')) return;

    document.querySelectorAll('.post-date-relative[data-post-ts]').forEach(function (el) {
        var ts = parseInt(el.getAttribute('data-post-ts'), 10);
        if (!ts) return;
        el.textContent = timeAgo(ts);
    });

    if (location.hash && /^#post-\d+$/.test(location.hash)) {
        var id = location.hash.replace('#post-', '');
        requestAnimationFrame(function () {
            var target = document.getElementById('post-' + id);
            if (target) {
                target.scrollIntoView({ behavior: 'smooth', block: 'center' });
                target.classList.add('post-highlight-flash');
                setTimeout(function () { target.classList.remove('post-highlight-flash'); }, 2000);
            }
        });
    }

    var ta = document.querySelector('.page-home .quick-post-textarea');
    var counter = document.querySelector('.page-home .quick-post-char-count');
    var maxLen = 8000;
    var draftKey = 'expressify_quick_post_draft';
    if (ta) {
        try {
            var saved = localStorage.getItem(draftKey);
            if (saved && !(ta.value || '').trim()) {
                ta.value = saved;
            }
        } catch (e) { /* ignore */ }
    }
    if (ta && counter) {
        function updateCount() {
            var n = (ta.value || '').length;
            counter.textContent = n + ' / ' + maxLen;
        }
        ta.addEventListener('input', updateCount);
        updateCount();
    }
    if (ta) {
        var draftTimer;
        ta.addEventListener('input', function () {
            clearTimeout(draftTimer);
            draftTimer = setTimeout(function () {
                try {
                    localStorage.setItem(draftKey, ta.value || '');
                } catch (e) { /* ignore */ }
            }, 400);
        });
        var qForm = ta.closest('form');
        if (qForm) {
            qForm.addEventListener('submit', function () {
                try {
                    localStorage.removeItem(draftKey);
                } catch (e) { /* ignore */ }
            });
        }
        ta.addEventListener('keydown', function (e) {
            if (e.ctrlKey && e.key === 'Enter') {
                e.preventDefault();
                var form = ta.closest('form');
                if (form && form.checkValidity()) {
                    form.requestSubmit();
                } else if (form) {
                    form.reportValidity();
                }
            }
        });
    }
}

function copyPostLink(postId) {
    var url = window.location.origin + '/home#post-' + postId;
    if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(url).then(function () {
            showNotification('Link copied to clipboard', 'success');
        }).catch(function () {
            fallbackCopy(url);
        });
    } else {
        fallbackCopy(url);
    }
}

function fallbackCopy(text) {
    var ta = document.createElement('textarea');
    ta.value = text;
    ta.style.position = 'fixed';
    ta.style.left = '-9999px';
    document.body.appendChild(ta);
    ta.select();
    try {
        document.execCommand('copy');
        showNotification('Link copied to clipboard', 'success');
    } catch (e) {
        showNotification('Could not copy link', 'error');
    }
    document.body.removeChild(ta);
}
window.copyPostLink = copyPostLink;

const REPORT_REASONS = [
    "Spam",
    "Harassment or bullying",
    "Hate speech",
    "Nudity or sexual content",
    "Violence or dangerous acts",
    "Misinformation",
    "Other"
];

function openReportMenu(btn) {
    const postEl = btn.closest('.post');
    if (!postEl) return;
    let menu = postEl.querySelector('.report-menu');
    if (!menu) {
        menu = document.createElement('div');
        menu.className = 'report-menu';
        menu.innerHTML = REPORT_REASONS.map(function (reason) {
            return '<button type="button" class="report-reason-btn" data-reason="' +
                reason.replace(/"/g, '&quot;') + '">' + reason + '</button>';
        }).join('');
        postEl.appendChild(menu);
        menu.addEventListener('click', function (e) {
            const reasonBtn = e.target.closest('.report-reason-btn');
            if (!reasonBtn) return;
            const reason = reasonBtn.getAttribute('data-reason');
            submitReport(postEl, reason);
        });
    }
    const isVisible = menu.classList.contains('show');
    document.querySelectorAll('.report-menu.show').forEach(function (el) {
        el.classList.remove('show');
    });
    if (!isVisible) {
        menu.classList.add('show');
    }
}
window.openReportMenu = openReportMenu;

document.addEventListener('click', function (e) {
    if (!e.target.closest('.report-post-btn') && !e.target.closest('.report-menu')) {
        document.querySelectorAll('.report-menu.show').forEach(function (el) {
            el.classList.remove('show');
        });
    }
});

function submitReport(postEl, reason) {
    const postId = postEl.getAttribute('data-post-id');
    if (!postId || !reason) return;
    const body = 'post_id=' + encodeURIComponent(postId) +
        '&reason=' + encodeURIComponent(reason);
    fetch('/api/report_post', fetchOpts('POST', body))
        .then(function (r) { return r.json(); })
        .then(function (data) {
            const menu = postEl.querySelector('.report-menu');
            if (menu) menu.classList.remove('show');
            if (data.success) {
                showNotification(data.message || 'Report submitted.', 'success');
            } else if (data.error) {
                showNotification(data.error, 'error');
            } else {
                showNotification('Failed to submit report.', 'error');
            }
        })
        .catch(function () {
            const menu = postEl.querySelector('.report-menu');
            if (menu) menu.classList.remove('show');
            showNotification('Failed to submit report.', 'error');
        });
}
