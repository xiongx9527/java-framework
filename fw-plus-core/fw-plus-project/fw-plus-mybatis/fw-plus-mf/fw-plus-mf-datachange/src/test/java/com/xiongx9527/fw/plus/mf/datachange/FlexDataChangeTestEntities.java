package com.xiongx9527.fw.plus.mf.datachange;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;

@Table(value = "audit_user", schema = "audit")
class FlexUserFixture {
	@Id("user_id")
	Long userId;
	@Column("display_name")
	String name;
	@Column(ignore = true)
	String transientValue;
	@Column(isLogicDelete = true)
	Integer deleted;
	@Column(value = "lock_version", version = true)
	Integer version;
	@Column(value = "created_at", onInsertValue = "now()")
	Long createdAt;
	@Column(value = "updated_at", onInsertValue = "now()", onUpdateValue = "now()")
	Long updatedAt;
}

@Table("other_flex_user")
class OtherFlexUserFixture {
	@Id("other_id")
	Long id;
}
