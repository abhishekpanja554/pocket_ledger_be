package com.abhout.pocket_ledger_be.state;

import com.abhout.pocket_ledger_be.document.DocumentRepository;
import com.abhout.pocket_ledger_be.document.DocumentResponse;
import com.abhout.pocket_ledger_be.rule.RuleRepository;
import com.abhout.pocket_ledger_be.rule.RuleResponse;
import com.abhout.pocket_ledger_be.setting.SettingRepository;
import com.abhout.pocket_ledger_be.setting.SettingService;
import com.abhout.pocket_ledger_be.setting.SettingsResponse;
import com.abhout.pocket_ledger_be.tag.TagRepository;
import com.abhout.pocket_ledger_be.tag.TagResponse;
import com.abhout.pocket_ledger_be.transaction.TransactionRepository;
import com.abhout.pocket_ledger_be.transaction.TransactionResponse;
import com.abhout.pocket_ledger_be.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StateService {
    private final TagRepository tagRepository;
    private final TransactionRepository transactionRepository;
    private final RuleRepository ruleRepository;
    private final DocumentRepository documentRepository;
    private final SettingService settingService;

    private static final int MAX_TRANSACTIONS = 5000;

    @Transactional(readOnly = true)
    public StateResponse getState(User user){
        UUID userId = user.getId();
        Pageable transactionsPage = PageRequest.of(0,
                MAX_TRANSACTIONS,
                Sort.by(Sort.Order.desc("date"),
                        Sort.Order.desc("createdAt")));
        List<TransactionResponse> transactions = transactionRepository
                .findByUserId(userId, transactionsPage)
                .stream().map(TransactionResponse::from).toList();

        List<TagResponse> tags =
                tagRepository.findByUserIdOrderByNameAsc(userId)
                .stream().map(TagResponse::from).toList();

        List<RuleResponse> rules = ruleRepository
                .findByUserIdOrderByCreatedAtAsc(userId)
                .stream().map(RuleResponse::from).toList();

        List<DocumentResponse> documents = documentRepository
                .findTop100ByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(DocumentResponse::from).toList();

        SettingsResponse settings = settingService.decode(user);

        return new StateResponse(transactions, tags, rules,
                settings, documents);
    }

}
