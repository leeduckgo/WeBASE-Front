/*
 * Copyright 2014-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.webank.webase.front.transaction;

import static com.webank.webase.front.base.code.ConstantCode.ENCODE_STR_CANNOT_BE_NULL;
import static com.webank.webase.front.base.code.ConstantCode.INVALID_VERSION;
import static com.webank.webase.front.base.code.ConstantCode.PARAM_ADDRESS_IS_INVALID;
import static com.webank.webase.front.base.code.ConstantCode.PARAM_FAIL_CNS_NAME_IS_EMPTY;
import static com.webank.webase.front.base.code.ConstantCode.VERSION_AND_ADDRESS_CANNOT_ALL_BE_NULL;

import com.webank.webase.front.base.code.ConstantCode;
import com.webank.webase.front.base.controller.BaseController;
import com.webank.webase.front.base.exception.FrontException;
import com.webank.webase.front.transaction.entity.ReqQueryTransHandle;
import com.webank.webase.front.transaction.entity.ReqSignMessageHash;
import com.webank.webase.front.transaction.entity.ReqSignedTransHandle;
import com.webank.webase.front.transaction.entity.ReqTransHandle;
import com.webank.webase.front.transaction.entity.ReqTransHandleWithSign;
import com.webank.webase.front.util.Address;
import com.webank.webase.front.util.CommonUtils;
import com.webank.webase.front.util.JsonUtils;
import com.webank.webase.front.util.PrecompiledUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import java.time.Duration;
import java.time.Instant;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.fisco.bcos.sdk.model.TransactionReceipt;
import org.fisco.bcos.sdk.utils.Numeric;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TransController.
 * handle transactions with sign to deploy/call contract
 */
@Api(value = "/trans", tags = "transaction interface")
@Slf4j
@RestController
@RequestMapping(value = "/trans")
public class TransController extends BaseController {

    @Autowired
    TransService transServiceImpl;

    /**
     * transHandle through webase-sign
     * @return
     */
    @ApiOperation(value = "transaction handling", notes = "transaction handling")
    @ApiImplicitParam(name = "reqTransHandle", value = "transaction info", required = true, dataType = "ReqTransHandleWithSign")
    @PostMapping("/handleWithSign")
    public Object transHandle(@Valid @RequestBody ReqTransHandleWithSign reqTransHandle, BindingResult result) throws Exception {
        log.info("transHandle start. ReqTransHandleWithSign:[{}]", JsonUtils.toJSONString(reqTransHandle));

        Instant startTime = Instant.now();
        log.info("transHandle start startTime:{}", startTime.toEpochMilli());

        checkParamResult(result);
        String address = reqTransHandle.getContractAddress();
        if (StringUtils.isBlank(reqTransHandle.getVersion()) && StringUtils.isBlank(address)) {
            throw new FrontException(VERSION_AND_ADDRESS_CANNOT_ALL_BE_NULL);
        }
        if (!StringUtils.isBlank(address) && (address.length() != Address.ValidLen
                || org.fisco.bcos.sdk.abi.datatypes.Address.DEFAULT.toString().equals(address))) {
            throw new FrontException(PARAM_ADDRESS_IS_INVALID);
        }
        if (reqTransHandle.isUseCns()) {
            if (!PrecompiledUtils.checkVersion(reqTransHandle.getVersion())) {
                throw new FrontException(INVALID_VERSION);
            }
            if (StringUtils.isBlank(reqTransHandle.getCnsName())) {
                throw new FrontException(PARAM_FAIL_CNS_NAME_IS_EMPTY);
            }
        }
        Object obj =  transServiceImpl.transHandleWithSign(reqTransHandle);
        log.info("transHandle end  useTime:{}",
                Duration.between(startTime, Instant.now()).toMillis());
        return obj;
    }

    @ApiOperation(value = "transaction handle locally", notes = "transaction locally")
    @ApiImplicitParam(name = "reqTransHandle", value = "transaction info", required = true, dataType = "ReqTransHandle")
    @PostMapping("/handle")
    public Object transHandleLocal(@Valid @RequestBody ReqTransHandle reqTransHandle, BindingResult result) throws Exception {
        log.info("transHandleLocal start. ReqTransHandle:[{}]", JsonUtils.toJSONString(reqTransHandle));

        Instant startTime = Instant.now();
        log.info("transHandleLocal start startTime:{}", startTime.toEpochMilli());

        checkParamResult(result);
        String address = reqTransHandle.getContractAddress();
        if (StringUtils.isBlank(reqTransHandle.getVersion()) && StringUtils.isBlank(address)) {
            throw new FrontException(VERSION_AND_ADDRESS_CANNOT_ALL_BE_NULL);
        }
        if (!StringUtils.isBlank(address) && address.length() != Address.ValidLen) {
            throw new FrontException(PARAM_ADDRESS_IS_INVALID);
        }
        if (reqTransHandle.isUseCns()) {
            if (!PrecompiledUtils.checkVersion(reqTransHandle.getVersion())) {
                throw new FrontException(INVALID_VERSION);
            }
            if (StringUtils.isBlank(reqTransHandle.getCnsName())) {
                throw new FrontException(PARAM_FAIL_CNS_NAME_IS_EMPTY);
            }
        }
        Object obj =  transServiceImpl.transHandleLocal(reqTransHandle);
        log.info("transHandleLocal end  useTime:{}",
                Duration.between(startTime, Instant.now()).toMillis());
        return obj;
    }

    @ApiOperation(value = "get raw tx", notes = "get raw tx")
    @ApiImplicitParam(name = "reqTransHandle", value = "transaction info", required = true, dataType = "reqTransHandle")
    @PostMapping("/handleWithRaw")
    public Object transHandleLocal(@Valid @RequestBody ReqTransHandle reqTransHandle, BindingResult result) throws Exception {
        // Todo: Get Tx and Return Raw Tx with base64
    }

    @ApiOperation(value = "send signed transaction ")
    @ApiImplicitParam(name = "reqSignedTransHandle", value = "transaction info", required = true, dataType = "ReqSignedTransHandle")
    @PostMapping("/signed-transaction")
    public TransactionReceipt sendSignedTransaction(@Valid @RequestBody ReqSignedTransHandle reqSignedTransHandle, BindingResult result) throws Exception {
        log.info("transHandleLocal start. ReqSignedTransHandle:[{}]", JsonUtils.toJSONString(reqSignedTransHandle));

        Instant startTime = Instant.now();
        log.info("transHandleLocal start startTime:{}", startTime.toEpochMilli());

        checkParamResult(result);
        String signedStr = reqSignedTransHandle.getSignedStr();
        if (StringUtils.isBlank(signedStr)) {
            throw new FrontException(ENCODE_STR_CANNOT_BE_NULL);
        }
        TransactionReceipt receipt =  transServiceImpl.sendSignedTransaction(signedStr, reqSignedTransHandle.getSync(),reqSignedTransHandle.getGroupId());
        log.info("transHandleLocal end  useTime:{}", Duration.between(startTime, Instant.now()).toMillis());
        return receipt;
    }

    @ApiOperation(value = "send query transaction ")
    @ApiImplicitParam(name = "reqQueryTransHandle", value = "transaction info", required = true, dataType = "ReqQueryTransHandle")
    @PostMapping("/query-transaction")
    public Object sendQueryTransaction(@Valid @RequestBody ReqQueryTransHandle reqQueryTransHandle, BindingResult result)   {
        log.info("transHandleLocal start. ReqQueryTransHandle:[{}]", JsonUtils.toJSONString(reqQueryTransHandle));

        Instant startTime = Instant.now();
        log.info("transHandleLocal start startTime:{}", startTime.toEpochMilli());

        checkParamResult(result);
        String encodeStr = reqQueryTransHandle.getEncodeStr();
        if (StringUtils.isBlank(encodeStr)) {
            throw new FrontException(ENCODE_STR_CANNOT_BE_NULL);
        }
        Object obj =  transServiceImpl.sendQueryTransaction(encodeStr, reqQueryTransHandle.getContractAddress(),reqQueryTransHandle.getFuncName(),reqQueryTransHandle.getContractAbi(),reqQueryTransHandle.getGroupId(),reqQueryTransHandle.getUserAddress());
        log.info("transHandleLocal end  useTime:{}", Duration.between(startTime, Instant.now()).toMillis());
        return obj;
    }


    @ApiOperation(value = "sign Message locally", notes = "sign Message locally")
    @ApiImplicitParam(name = "reqSignMessageHash", value = "ReqSignMessageHash info", required = true, dataType = "ReqSignMessageHash")
    @PostMapping("/signMessageHash")
    public Object signMessageHash(@Valid @RequestBody ReqSignMessageHash reqSignMessageHash, BindingResult result) {
        log.info("transHandleLocal start. ReqTransHandle:[{}]", JsonUtils.toJSONString(reqSignMessageHash));
        checkParamResult(result);
        Instant startTime = Instant.now();
        log.info("transHandleLocal start startTime:{}", startTime.toEpochMilli());

        if(!CommonUtils.isHexNumber(Numeric.cleanHexPrefix(reqSignMessageHash.getHash())))
        {
            throw new FrontException(ConstantCode.GET_MESSAGE_HASH, "not a hexadecimal hash string");
        }
        if( Numeric.cleanHexPrefix(reqSignMessageHash.getHash()).length() != CommonUtils.HASH_LENGTH_64)
        {
            throw new FrontException(ConstantCode.GET_MESSAGE_HASH, "wrong length");
        }
        Object obj =  transServiceImpl.signMessageLocal(reqSignMessageHash);
        log.info("signMessageLocal end  useTime:{}",
                Duration.between(startTime, Instant.now()).toMillis());
        return obj;
    }

    /**
     * transHandle through webase-sign
     * @return
     */
    @ApiOperation(value = "transaction to raw tx str", notes = "transaction handling")
    @ApiImplicitParam(name = "reqTransHandle", value = "transaction info", required = true, dataType = "ReqTransHandle")
    @PostMapping("/convertSignedTrans")
    public String transToRawTxStrLocal(@Valid @RequestBody ReqTransHandle reqTransHandle, BindingResult result) throws Exception {
        log.info("transToRawTxStrLocal start. ReqTransHandle:[{}]", JsonUtils.toJSONString(reqTransHandle));

        Instant startTime = Instant.now();
        log.info("transToRawTxStrLocal start startTime:{}", startTime.toEpochMilli());

        checkParamResult(result);
        String address = reqTransHandle.getContractAddress();
        if (StringUtils.isBlank(reqTransHandle.getVersion()) && StringUtils.isBlank(address)) {
            throw new FrontException(VERSION_AND_ADDRESS_CANNOT_ALL_BE_NULL);
        }
        if (!StringUtils.isBlank(address) && address.length() != Address.ValidLen) {
            throw new FrontException(PARAM_ADDRESS_IS_INVALID);
        }
        if (reqTransHandle.isUseCns()) {
            if (!PrecompiledUtils.checkVersion(reqTransHandle.getVersion())) {
                throw new FrontException(INVALID_VERSION);
            }
            if (StringUtils.isBlank(reqTransHandle.getCnsName())) {
                throw new FrontException(PARAM_FAIL_CNS_NAME_IS_EMPTY);
            }
        }
        String encodedOrSignedResult =  transServiceImpl.transToRawTxStr(reqTransHandle);
        log.info("transToRawTxStrLocal end useTime:{},encodedOrSignedResult:{}",
            Duration.between(startTime, Instant.now()).toMillis(), encodedOrSignedResult);
        return encodedOrSignedResult;
    }


}
