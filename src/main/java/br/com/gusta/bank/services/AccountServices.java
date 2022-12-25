package br.com.gusta.bank.services;

import br.com.gusta.bank.controllers.*;
import br.com.gusta.bank.data.vo.v1.*;
import br.com.gusta.bank.data.vo.v1.security.*;
import br.com.gusta.bank.exceptions.*;
import br.com.gusta.bank.mapper.DozerMapper;
import br.com.gusta.bank.model.*;
import br.com.gusta.bank.repositories.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.*;
import org.springframework.stereotype.Service;

import java.util.*;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@Service
public class AccountServices implements UserDetailsService {

	@Autowired
	AccountRepository repository;
	String accountName;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		var account = repository.findByUsername(username);
		if (account == null) throw new UsernameNotFoundException("User not found!");
		accountName = account.getAccountName();
		return new CustomUserDetails(account);
	}

	public AccountVO createAccount(AccountVO accountVO) {
		if (accountVO.getAccountName() == null || accountVO.getAccountPassword() == null) throw new NullPointerException("Not possible create a account with null fields");
		if (accountVO.getAccountName().isBlank() || accountVO.getAccountPassword().isBlank()) throw new IllegalArgumentException("Not possible create a account with empty fields");
		if (accountVO.getAccountBalance() == null || accountVO.getAccountBalance().toString().isBlank()) accountVO.setAccountBalance(0D);

		var verifyDoesNotExists = repository.findByUsername(accountVO.getAccountName());
		if(verifyDoesNotExists != null) throw new RepeatedAccountException();

		Account entity = DozerMapper.parseObject(accountVO, Account.class);
		entity.setPermissions(List.of(new Permission(1L)));
		entity.setAccountPassword(new BCryptPasswordEncoder(10).encode(accountVO.getAccountPassword()));
		repository.save(entity);

		accountVO
				.add(
						linkTo(methodOn(AuthController.class)
							.login(new AccountCredentialsVO()))
							.withSelfRel()
				);
		accountVO
				.add(
						linkTo(methodOn(AccountController.class)
							.deposit(new DepositVO()))
							.withSelfRel()
				);

		return accountVO;
	}

	public String deposit(DepositVO deposit) {
		if (deposit.getAccountName() == null || deposit.getDepositValue() == null) throw new NullPointerException("Enter a value in the parameters");
		if (deposit.getAccountName().isBlank() || deposit.getDepositValue().toString().isBlank()) throw new IllegalArgumentException("Enter a valid value in the parameters");
		if (deposit.getDepositValue() <= 0) throw new InvalidValueException();

		Account entity = repository.findByUsername(deposit.getAccountName());
		if (entity == null) throw new RequiredObjectIsNullException("This account does not exists, please register in /api/bank/v1/create");

		entity.setAccountBalance(entity.getAccountBalance() + deposit.getDepositValue());
		repository.save(entity);
		return "the deposit is completed";
	}// end of the deposit method

	public String transfer(TransferVO transfer) {

		Account originEntity = repository.findByUsername(accountName);
		if (transfer.getDestinyAccountName().equalsIgnoreCase(originEntity.getAccountName())) throw new IllegalArgumentException("Not possible transfer money to yourself");
		if (transfer.getValueTransfer() > originEntity.getAccountBalance() || transfer.getValueTransfer() <= 0) throw new InvalidValueException();

		if (transfer.getDestinyAccountName() == null || transfer.getValueTransfer() == null) throw new NullPointerException("Enter a value in the parameters");
		if (transfer.getDestinyAccountName().isBlank() || transfer.getValueTransfer().toString().isBlank()) throw new IllegalArgumentException("Enter a valid value in the parameters");

		Account destinyEntity = repository.findByUsername(transfer.getDestinyAccountName());
		if (destinyEntity == null) throw new RequiredObjectIsNullException();

		originEntity.setAccountBalance(originEntity.getAccountBalance() - transfer.getValueTransfer());
		destinyEntity.setAccountBalance(destinyEntity.getAccountBalance() + transfer.getValueTransfer());
		repository.save(originEntity);
		repository.save(destinyEntity);
		return "Transfer successful";
	}// end of the transfer method

}