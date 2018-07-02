package com.zealens.listory.alipay;

/**
 * Created by songkang on 2018/4/20.
 */

public class AlipayConfig {
    /** 支付宝支付业务：入参app_id */
    public static final String APPID = "2018041702572070";

    /** 支付宝账户登录授权业务：入参pid值 */
    public static final String PID = "2088002315122905";
    /** 支付宝账户登录授权业务：入参target_id值 */
    public static final String TARGET_ID = "226288";

    /** 商户私钥，pkcs8格式 */
    /** 如下私钥，RSA2_PRIVATE 或者 RSA_PRIVATE 只需要填入一个 */
    /** 如果商户两个都设置了，优先使用 RSA2_PRIVATE */
    /** RSA2_PRIVATE 可以保证商户交易在更加安全的环境下进行，建议使用 RSA2_PRIVATE */
    /** 获取 RSA2_PRIVATE，建议使用支付宝提供的公私钥生成工具生成， */
    /** 工具地址：https://doc.open.alipay.com/docs/doc.htm?treeId=291&articleId=106097&docType=1 */
    public static final String RSA2_PRIVATE = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDKeymuJnTdgguabHTY/gJ+A6x4EfdmTbyw1ILxL3rcD1l0zRV3ycnQ0XuKJbPqBR9oS7qxVEZK7L7apLW9UdPfCWAxQcnLu1yPKsyeeiLT2f3TY17YlH7AW3UoJjRaBQKAByU7iOtWPSX0IXlxP78c5eLfhjQqNoyDtW3U9psqujqzh2ChKcpp9KMVZUEJ0Pjht33XK8kpofUTup56gBXKK+fm/n9x7ruXwPdTioXEllH6MM2nJC/fEtLMqVp42u+Kccrv3O1wpiy8cs8sQ7vZN2wKrMahWSvl5fdHPWl+wOYyScS+9/h/xCWKRoM3YO40N5KZV8zFCs8kQMPLxrF9AgMBAAECggEAaIBoYpk2d5xGoMyZAeUXJDKBMPcU2JgGar1DV3dJ0Ymagc3W5pwerMdxGzuaP42lSA7Nq/fyz3igh/e6VJuG3fXYz5NEswFgJAPS13fxwaUge5ktulnpkR2iubX+DvfG8oy9ipx/2WJ11Cnyo6Bi4mdDUFnTUnYUz2Uz6lXcf4/y+CjUNvZSeHzd3x/XEhJtwRFp/tzKi/LHHfaxIYPldQrxgdidgzzMPcGeRgnFByuYPeY5ZhV2PjDDAID7HFK0eRmpIL11JiUEEovIFRnnyvQgKnY9fW0ScGUSimcL63IhqvI3fL/ki2j9SW43fhDUUx8+My9jy2eqqttEPE/0AQKBgQDlD6kwIsb2vA5TW5Jo5gfaTB1I3klJml15I8PeGY0XsGKGM3IupUwpvHeYVY8yneEYD5I4GthdWK+/+RR29CFWFAxlk5t4AciBGYyx9wCdbgvyQsWpNnJVUPtDmLhe39x3F9Wu8JwXXwQleYZJBzYMwxeZOr2tdS2qB5b8rVUNyQKBgQDiS0GjS7AEAZU/xXdebCFHnCXeoKyo6Khdx0qtyecSf6BmsM6uj2cdZu2b95+w/p604S97ij8DirGJIQ3G9bm268Dtwit+2fXQB4gu4bCdSr8hY/LsGmm+3aO3cJD7dvHlzQr0RRmCGhu57oOr7UI7/i0p6UbgI3SXh6sd43AQFQKBgQDbvyJ4Y2Ss38x+e6MBr2xpKAwQE/ALBGQYCw+kyq5ow3ZBeKlZxopCn+O76Jjv3jZLduS3nkljg+lXKgVP+PTiN2mNKQZFIrDP6UrokCWu4hvj1dr6cplM79udBa4alnhSaDM/GOUmJqk7qlIBTUG1DJRfE9NTzM56B735ue+WwQKBgCP2T70Stys70SssR3cS+gr2TrJdmHLTXoZguIZ70qQG8ke/i2kEUx/CDrq9lHw9Sp12N+SLKM54SRSA0z6uKFBgkO138Q+ctcrUl5jTtwyYJ124E4/f9rQlZvItCm1cDr81YatGG/KoEYXv2K8G6oK8NxM1BlVybQHzxobecR2tAoGBAK5Q/0WMb1mhXrcjwrSg9weDpNnoQKKqfo63evzwsEWiOeQQc4rhaiZbSypCPbGTYrcFadV/3kOcFbtJyCpKCXbWebyocTOVQYJdmxdBjcGnHhZILj8sx/I6041/6ERdSKCTcfsTNMjvYnvc2CzPAM6fv79GqfZCiF40odArvJyc";
    public static final String RSA_PRIVATE = "";

    public static final int SDK_PAY_FLAG = 1;
    public static final int SDK_AUTH_FLAG = 2;
}
