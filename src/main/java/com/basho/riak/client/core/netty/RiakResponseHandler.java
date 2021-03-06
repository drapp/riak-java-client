/*
 * Copyright 2013 Basho Technologies Inc.
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
package com.basho.riak.client.core.netty;

import com.basho.riak.client.core.RiakMessage;
import com.basho.riak.client.core.RiakResponseListener;
import com.basho.riak.client.util.RiakMessageCodes;
import com.basho.riak.protobuf.RiakPB;
import com.google.protobuf.ByteString;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 *
 * @author Brian Roach <roach at basho dot com>
 * @since 2.0
 */
public class RiakResponseHandler extends ChannelInboundHandlerAdapter
{

    private RiakResponseListener listener;
    
    public RiakResponseHandler()
    {
        super();
    }
    
    public void setListener(RiakResponseListener listener)
    {
        this.listener = listener;
    }
    
    @Override
    public void channelRead(ChannelHandlerContext chc, Object message) throws Exception
    {
        RiakMessage riakMessage = (RiakMessage) message;
        if (riakMessage.getCode() == RiakMessageCodes.MSG_ErrorResp)
        {
            RiakPB.RpbErrorResp error = RiakPB.RpbErrorResp.newBuilder()
                                                .setErrcode(riakMessage.getCode())
                                                .setErrmsg(ByteString.copyFrom(riakMessage.getData()))
                                                .build(); 
            listener.onException(chc.channel(), new RiakResponseException(RiakMessageCodes.MSG_ErrorResp, error.getErrmsg().toStringUtf8()));
        }
        else
        {
            listener.onSuccess(chc.channel(), riakMessage);
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception 
    {
        // On any exception in the pipeline we explitly close the context here 
        // so the channel doesn't get reused by the ConnectionPool. 
        ctx.close();
        listener.onException(ctx.channel(), cause);
    }
    
}
